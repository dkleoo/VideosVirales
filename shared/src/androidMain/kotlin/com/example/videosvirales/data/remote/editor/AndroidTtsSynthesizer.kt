package com.example.videosvirales.data.remote.editor

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.videosvirales.data.mappers.editor.EditorError
import com.example.videosvirales.data.repositoyImpl.editor.EditorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidTtsSynthesizer(
    private val context: Context,
) {
    suspend fun synthesize(text: String): File = withContext(Dispatchers.Main) {
        val engine = initialize()
        try {
            configureLanguage(engine)
            val file = File(context.cacheDir, "narration_${System.currentTimeMillis()}.wav")
            val utteranceId = "narration_${System.nanoTime()}"
            val completed = withTimeoutOrNull(SYNTHESIS_TIMEOUT_MS) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    engine.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onDone(utteranceId: String?) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        EditorException(EditorError.TTS_FAILED)
                                    )
                                }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        EditorException(EditorError.TTS_FAILED)
                                    )
                                }
                            }
                        }
                    )
                    val params = Bundle().apply {
                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                    }
                    val status = engine.synthesizeToFile(
                        text,
                        params,
                        file,
                        utteranceId,
                    )
                    if (status != TextToSpeech.SUCCESS && continuation.isActive) {
                        continuation.resumeWithException(EditorException(EditorError.TTS_FAILED))
                    }
                }
            }
            if (completed == null || !file.isFile || file.length() == 0L) {
                file.delete()
                throw EditorException(EditorError.TTS_FAILED)
            }
            file
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TTS_FAILED, exception)
        } finally {
            engine.shutdown()
        }
    }

    private suspend fun initialize(): TextToSpeech {
        var engine: TextToSpeech? = null
        return try {
            suspendCancellableCoroutine { continuation ->
                val listener = TextToSpeech.OnInitListener { status ->
                    val current = engine
                    if (current == null) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(EditorException(EditorError.TTS_FAILED))
                        }
                    } else if (status == TextToSpeech.SUCCESS) {
                        if (continuation.isActive) continuation.resume(current)
                    } else {
                        current.shutdown()
                        if (continuation.isActive) {
                            continuation.resumeWithException(EditorException(EditorError.TTS_FAILED))
                        }
                    }
                }
                val created = TextToSpeech(context, listener)
                engine = created
                continuation.invokeOnCancellation { created.shutdown() }
            }
        } catch (exception: EditorException) {
            throw exception
        } catch (exception: Exception) {
            throw EditorException(EditorError.TTS_FAILED, exception)
        }
    }

    private fun configureLanguage(engine: TextToSpeech) {
        val locale = Locale("es", "ES")
        var result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            result = engine.setLanguage(Locale("es", "MX"))
        }
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            throw EditorException(EditorError.TTS_FAILED)
        }

        val bestVoice = engine.voices
            ?.filter { voice -> voice.locale.language == "es" }
            ?.maxWithOrNull(
                compareBy<android.speech.tts.Voice> { voice -> voice.quality }
                    .thenBy { voice -> !voice.isNetworkConnectionRequired }
            )
        if (bestVoice != null) {
            engine.voice = bestVoice
        }
    }

    private companion object {
        const val SYNTHESIS_TIMEOUT_MS = 60_000L
    }
}
