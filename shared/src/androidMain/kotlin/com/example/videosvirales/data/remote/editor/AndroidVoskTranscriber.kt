package com.example.videosvirales.data.remote.editor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.mappers.editor.VideoFile
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipInputStream

class AndroidVoskTranscriber(
    private val context: Context,
) {
    @Volatile
    private var cachedModel: Model? = null
    @Volatile
    private var cachedModelPath: String? = null

    suspend fun transcribe(
        video: VideoFile,
        startMs: Long,
        endMs: Long,
    ): String = withContext(Dispatchers.IO) {
        try {
            val pcm = decodePcm(video, startMs, endMs) ?: return@withContext ""
            val model = getModel()
            val recognizer = Recognizer(model, SAMPLE_RATE.toFloat())
            try {
                recognizer.acceptWaveForm(pcm, pcm.size)
                val result = recognizer.finalResult
                JSONObject(result).optString("text").trim()
            } finally {
                recognizer.close()
            }
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TRANSCRIPTION_FAILED, exception)
        }
    }

    @Synchronized
    private fun getModel(): Model {
        val modelInfo = modelForLanguage()
        val modelDirectory = File(context.filesDir, "vosk/${modelInfo.id}")
        val current = cachedModel
        if (current != null && cachedModelPath == modelDirectory.absolutePath) return current

        if (!File(modelDirectory, "am").isDirectory) {
            modelDirectory.deleteRecursively()
            downloadAndExtract(modelInfo, modelDirectory)
        }
        val model = Model(modelDirectory.absolutePath)
        cachedModel?.close()
        cachedModel = model
        cachedModelPath = modelDirectory.absolutePath
        return model
    }

    private fun downloadAndExtract(modelInfo: VoskModelInfo, destination: File) {
        destination.parentFile?.mkdirs()
        val zipFile = File(context.cacheDir, "${modelInfo.id}.zip")
        try {
            val connection = (URL(modelInfo.url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 180_000
                requestMethod = "GET"
            }
            if (connection.responseCode !in 200..299) {
                throw EditorException(EditorError.TRANSCRIPTION_FAILED)
            }
            connection.inputStream.use { input ->
                zipFile.outputStream().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }

            val baseDirectory = destination.parentFile ?: context.filesDir
            destination.deleteRecursively()
            val basePath = baseDirectory.canonicalPath + File.separator
            ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val target = File(baseDirectory, entry.name)
                    if (!target.canonicalPath.startsWith(basePath)) {
                        throw EditorException(EditorError.TRANSCRIPTION_FAILED)
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output -> zip.copyTo(output, DEFAULT_BUFFER_SIZE) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            if (!File(destination, "am").isDirectory) {
                destination.deleteRecursively()
                throw EditorException(EditorError.TRANSCRIPTION_FAILED)
            }
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TRANSCRIPTION_FAILED, exception)
        } finally {
            zipFile.delete()
        }
    }

    private fun modelForLanguage(): VoskModelInfo =
        when (Locale.getDefault().language) {
            "es" -> VoskModelInfo(
                id = "vosk-model-small-es-0.42",
                url = "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip",
            )
            "pt" -> VoskModelInfo(
                id = "vosk-model-small-pt-0.3",
                url = "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip",
            )
            else -> VoskModelInfo(
                id = "vosk-model-small-en-us-0.15",
                url = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
            )
        }

    private fun decodePcm(
        video: VideoFile,
        startMs: Long,
        endMs: Long,
    ): ByteArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, Uri.parse(video.uri), null)
            var trackIndex = -1
            var inputFormat: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    trackIndex = index
                    inputFormat = format
                    break
                }
            }
            if (trackIndex < 0 || inputFormat == null) return null
            val sourceFormat = inputFormat

            extractor.selectTrack(trackIndex)
            extractor.seekTo(startMs * 1_000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val mime = sourceFormat.getString(MediaFormat.KEY_MIME) ?: return null
            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(sourceFormat, null, null, 0)
            decoder.start()

            val output = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var sampleRate = sourceFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE)
            var channelCount = sourceFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, 1)
            var inputDone = false
            var outputDone = false
            var attempts = 0

            while (!outputDone && attempts < MAX_DECODE_ATTEMPTS) {
                attempts++
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        if (inputBuffer == null) {
                            inputDone = true
                        } else {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            val sampleTime = extractor.sampleTime
                            if (sampleSize < 0 || sampleTime < 0 || sampleTime > endMs * 1_000) {
                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = decoder.outputFormat
                        sampleRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        channelCount = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, channelCount)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> {
                        if (outputIndex >= 0) {
                            val outputBuffer = decoder.getOutputBuffer(outputIndex)
                            if (outputBuffer != null &&
                                bufferInfo.size > 0 &&
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                                bufferInfo.presentationTimeUs >= startMs * 1_000 &&
                                bufferInfo.presentationTimeUs <= endMs * 1_000
                            ) {
                                val chunk = ByteArray(bufferInfo.size)
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.get(chunk)
                                output.write(chunk)
                            }
                            decoder.releaseOutputBuffer(outputIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
            }

            if (output.size() == 0) return null
            return resampleTo16k(
                bytes = output.toByteArray(),
                inputSampleRate = sampleRate,
                inputChannels = channelCount,
            )
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TRANSCRIPTION_FAILED, exception)
        } finally {
            try {
                codec?.stop()
            } catch (exception: Exception) {
                Unit
            }
            codec?.release()
            extractor.release()
        }
    }

    private fun resampleTo16k(
        bytes: ByteArray,
        inputSampleRate: Int,
        inputChannels: Int,
    ): ByteArray {
        if (inputSampleRate == SAMPLE_RATE && inputChannels == 1) return bytes
        if (inputSampleRate <= 0 || inputChannels <= 0) return bytes

        val inputFrames = bytes.size / (2 * inputChannels)
        if (inputFrames <= 0) return ByteArray(0)
        val outputFrames = (inputFrames.toLong() * SAMPLE_RATE / inputSampleRate).toInt()
        if (outputFrames <= 0) return ByteArray(0)

        val mono = FloatArray(inputFrames)
        for (frame in 0 until inputFrames) {
            var sum = 0
            for (channel in 0 until inputChannels) {
                val index = (frame * inputChannels + channel) * 2
                val low = bytes[index].toInt() and 0xFF
                val high = bytes[index + 1].toInt()
                sum += (high shl 8) or low
            }
            mono[frame] = sum.toFloat() / inputChannels
        }

        val output = ByteArray(outputFrames * 2)
        for (frame in 0 until outputFrames) {
            val sourcePosition = frame.toDouble() * inputSampleRate / SAMPLE_RATE
            val first = sourcePosition.toInt().coerceIn(0, inputFrames - 1)
            val second = (first + 1).coerceAtMost(inputFrames - 1)
            val fraction = (sourcePosition - first).toFloat()
            val sample = mono[first] + (mono[second] - mono[first]) * fraction
            val value = sample.toInt().coerceIn(-32_768, 32_767)
            output[frame * 2] = (value and 0xFF).toByte()
            output[frame * 2 + 1] = (value shr 8 and 0xFF).toByte()
        }
        return output
    }

    private fun MediaFormat.intOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private data class VoskModelInfo(
        val id: String,
        val url: String,
    )

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val MAX_DECODE_ATTEMPTS = 100_000
    }
}
