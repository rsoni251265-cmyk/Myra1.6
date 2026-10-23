package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(
    private val context: Context,
    private val onSpeechStart: () -> Unit = {},
    private val onSpeechDone: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch: Float = 1.15f
    private var currentRate: Float = 1.0f
    private var currentLanguage: String = "Hinglish"

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupVoiceParameters()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    onSpeechStart()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeechDone()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeechDone()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onSpeechDone()
                }
            })
            Log.d("TtsManager", "TTS successfully initialized")
        } else {
            Log.e("TtsManager", "TTS initialization failed: $status")
        }
    }

    fun updateVoiceSettings(pitch: Float, rate: Float, language: String) {
        currentPitch = pitch
        currentRate = rate
        currentLanguage = language
        if (isInitialized) {
            setupVoiceParameters()
        }
    }

    private fun setupVoiceParameters() {
        tts?.setPitch(currentPitch)
        tts?.setSpeechRate(currentRate)

        val locale = when (currentLanguage) {
            "Hindi" -> Locale("hi", "IN")
            "English" -> Locale.US
            else -> Locale("en", "IN") // Hinglish/Indian English
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
    }

    fun speak(text: String, utteranceId: String = System.currentTimeMillis().toString()) {
        if (!isInitialized) return
        stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TtsManager", "Error shutting down TTS", e)
        }
    }
}
