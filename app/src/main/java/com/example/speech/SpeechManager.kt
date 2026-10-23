package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface SpeechEventListener {
    fun onWakeWordDetected(phrase: String)
    fun onCommandReceived(command: String)
    fun onPartialTranscription(partialText: String)
    fun onAudioLevelChanged(rmsDb: Float)
    fun onListeningStateChanged(isListening: Boolean)
    fun onErrorOccurred(errorMessage: String)
}

class SpeechManager(
    private val context: Context,
    private val listener: SpeechEventListener
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningInternal = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var wakePhrase: String = "Hello Heena"
    private var language: String = "Hinglish"
    private var wakeWordModeOnly: Boolean = false

    fun updateConfig(wakePhrase: String, language: String) {
        this.wakePhrase = wakePhrase
        this.language = language
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun ensureRecognizer() {
        if (speechRecognizer == null && isRecognitionAvailable()) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        listener.onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioLevel.value = normalized
                        listener.onAudioLevelChanged(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        listener.onListeningStateChanged(false)
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioLevel.value = 0f
                        listener.onListeningStateChanged(false)

                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again!"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error ($error)"
                        }
                        Log.w("SpeechManager", "Speech error: $errorMsg")
                        // For no match or timeout in continuous wake-word mode, restart listening
                        if (wakeWordModeOnly && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                            startWakeWordListening()
                        } else {
                            listener.onErrorOccurred(errorMsg)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioLevel.value = 0f
                        listener.onListeningStateChanged(false)

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0].trim()
                            Log.d("SpeechManager", "Recognized: $recognized")
                            handleRecognizedText(recognized)
                        } else {
                            if (wakeWordModeOnly) {
                                startWakeWordListening()
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partial = matches[0]
                            listener.onPartialTranscription(partial)
                            checkWakeWordInPartial(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun checkWakeWordInPartial(partial: String) {
        val lower = partial.lowercase()
        val wakeClean = wakePhrase.lowercase()
        if (lower.contains(wakeClean) || lower.contains("heena") || lower.contains("hello heena") || lower.contains("namaste heena")) {
            wakeWordModeOnly = false
            listener.onWakeWordDetected(partial)
        }
    }

    private fun handleRecognizedText(text: String) {
        val lower = text.lowercase()
        val wakeClean = wakePhrase.lowercase()

        if (wakeWordModeOnly) {
            if (lower.contains(wakeClean) || lower.contains("heena") || lower.contains("hello heena") || lower.contains("namaste heena")) {
                wakeWordModeOnly = false
                val remainingCommand = text
                    .replace(Regex("(?i)hello\\s+heena"), "")
                    .replace(Regex("(?i)namaste\\s+heena"), "")
                    .replace(Regex("(?i)heena"), "")
                    .trim()
                if (remainingCommand.isNotEmpty()) {
                    listener.onCommandReceived(remainingCommand)
                } else {
                    listener.onWakeWordDetected(text)
                }
            } else {
                // Keep listening for wake phrase
                startWakeWordListening()
            }
        } else {
            // In active command mode
            val cleanCommand = text
                .replace(Regex("(?i)hello\\s+heena[\\s,]*"), "")
                .replace(Regex("(?i)namaste\\s+heena[\\s,]*"), "")
                .replace(Regex("(?i)heena[\\s,]*"), "")
                .trim()
            val finalQuery = if (cleanCommand.isNotBlank()) cleanCommand else text
            listener.onCommandReceived(finalQuery)
        }
    }

    fun startListeningForCommand() {
        wakeWordModeOnly = false
        startListeningInternal()
    }

    fun startWakeWordListening() {
        wakeWordModeOnly = true
        startListeningInternal()
    }

    private fun startListeningInternal() {
        ensureRecognizer()
        if (speechRecognizer == null) {
            listener.onErrorOccurred("Speech recognizer unavailable on this device")
            return
        }

        try {
            speechRecognizer?.cancel()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                val localeStr = when (language) {
                    "Hindi" -> "hi-IN"
                    "English" -> "en-US"
                    else -> "en-IN" // Hinglish/Indian English
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeStr)
            }

            speechRecognizer?.startListening(intent)
            isListeningInternal = true
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to start speech recognizer", e)
            _isListening.value = false
            listener.onErrorOccurred("Could not start speech recognition: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error stopping recognizer", e)
        }
        _isListening.value = false
        _audioLevel.value = 0f
        listener.onListeningStateChanged(false)
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error cancelling recognizer", e)
        }
        _isListening.value = false
        _audioLevel.value = 0f
        listener.onListeningStateChanged(false)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error destroying speech recognizer", e)
        }
    }
}
