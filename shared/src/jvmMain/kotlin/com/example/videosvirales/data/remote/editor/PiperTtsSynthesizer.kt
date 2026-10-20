package com.example.videosvirales.data.remote.editor

import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import io.github.givimad.piperjni.PiperConfig
import io.github.givimad.piperjni.PiperJNI
import io.github.givimad.piperjni.PiperVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class PiperTtsSynthesizer {
    suspend fun synthesize(text: String): File = withContext(Dispatchers.IO) {
        val model = modelForLanguage()
        try {
            val modelFile = ensureModel(model)
            val configFile = File(modelFile.absolutePath + ".json")
            val piper = PiperJNI()
            var config: PiperConfig? = null
            var voice: PiperVoice? = null
            try {
                config = piper.createConfig()
                voice = piper.loadVoice(
                    config,
                    modelFile.toPath(),
                    configFile.toPath(),
                    model.speakerId,
                )
                config.initialize(voice)
                val samples = piper.textToAudio(config, voice, text)
                if (samples.isEmpty()) throw EditorException(EditorError.TTS_FAILED)
                val file = File(
                    System.getProperty("java.io.tmpdir"),
                    "videosvirales_piper_${System.nanoTime()}.wav",
                )
                writeWav(file, samples, voice.sampleRate)
                file
            } finally {
                try {
                    voice?.close()
                } catch (exception: Exception) {
                    Unit
                }
                try {
                    config?.close()
                } catch (exception: Exception) {
                    Unit
                }
            }
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TTS_FAILED, exception)
        }
    }

    private fun ensureModel(model: PiperModel): File {
        val directory = File(
            System.getProperty("user.home"),
            ".videosvirales/piper/${model.id}",
        ).apply { mkdirs() }
        val modelFile = File(directory, "${model.id}.onnx")
        val configFile = File(directory, "${model.id}.onnx.json")
        if (!modelFile.isFile || modelFile.length() < MIN_MODEL_BYTES) {
            download(model.modelUrl, modelFile)
        }
        if (!configFile.isFile || configFile.length() < MIN_CONFIG_BYTES) {
            download(model.configUrl, configFile)
        }
        return modelFile
    }

    private fun download(url: String, destination: File) {
        val temporary = File(destination.parentFile, "${destination.name}.part")
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 180_000
                requestMethod = "GET"
            }
            if (connection.responseCode !in 200..299) {
                throw EditorException(EditorError.TTS_FAILED)
            }
            connection.inputStream.use { input ->
                temporary.outputStream().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
            if (temporary.length() == 0L) throw EditorException(EditorError.TTS_FAILED)
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
            }
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TTS_FAILED, exception)
        } finally {
            temporary.delete()
        }
    }

    private fun modelForLanguage(): PiperModel = PiperModel(
        id = "es_ES-davefx-medium",
        modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/" +
            "es/es_ES/davefx/medium/es_ES-davefx-medium.onnx?download=true",
        configUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/" +
            "es/es_ES/davefx/medium/es_ES-davefx-medium.onnx.json?download=true",
        speakerId = -1,
    )

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        val dataSize = samples.size * 2
        DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
            output.writeBytes("RIFF")
            output.writeIntLe(36 + dataSize)
            output.writeBytes("WAVE")
            output.writeBytes("fmt ")
            output.writeIntLe(16)
            output.writeShortLe(1)
            output.writeShortLe(1)
            output.writeIntLe(sampleRate)
            output.writeIntLe(sampleRate * 2)
            output.writeShortLe(2)
            output.writeShortLe(16)
            output.writeBytes("data")
            output.writeIntLe(dataSize)
            samples.forEach { sample -> output.writeShortLe(sample.toInt()) }
        }
    }

    private fun DataOutputStream.writeIntLe(value: Int) {
        write(value and 0xFF)
        write(value shr 8 and 0xFF)
        write(value shr 16 and 0xFF)
        write(value shr 24 and 0xFF)
    }

    private fun DataOutputStream.writeShortLe(value: Int) {
        write(value and 0xFF)
        write(value shr 8 and 0xFF)
    }

    private data class PiperModel(
        val id: String,
        val modelUrl: String,
        val configUrl: String,
        val speakerId: Long,
    )

    private companion object {
        const val MIN_MODEL_BYTES = 10_000_000L
        const val MIN_CONFIG_BYTES = 500L
    }
}
