package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AssistantSettings
import com.example.model.AssistantTheme
import com.example.model.CharacterOutfit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("heena_ai_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    private fun loadSettings(): AssistantSettings {
        val outfitId = prefs.getString("outfit", CharacterOutfit.CYBER_TECH.id) ?: CharacterOutfit.CYBER_TECH.id
        val outfit = CharacterOutfit.values().firstOrNull { it.id == outfitId } ?: CharacterOutfit.CYBER_TECH

        val themeId = prefs.getString("theme", AssistantTheme.CYBER_NEON.id) ?: AssistantTheme.CYBER_NEON.id
        val theme = AssistantTheme.values().firstOrNull { it.id == themeId } ?: AssistantTheme.CYBER_NEON

        return AssistantSettings(
            assistantName = prefs.getString("assistant_name", "Heena AI") ?: "Heena AI",
            wakePhrase = prefs.getString("wake_phrase", "Hello Heena") ?: "Hello Heena",
            language = prefs.getString("language", "Hinglish") ?: "Hinglish",
            speechRate = prefs.getFloat("speech_rate", 1.0f),
            pitch = prefs.getFloat("pitch", 1.15f),
            backgroundListening = prefs.getBoolean("bg_listening", false),
            selectedOutfit = outfit,
            selectedTheme = theme,
            autoSpeak = prefs.getBoolean("auto_speak", true),
            voiceEnrolled = prefs.getBoolean("voice_enrolled", false),
            enrolledUserName = prefs.getString("enrolled_user_name", "Master") ?: "Master"
        )
    }

    fun updateAssistantName(name: String) {
        prefs.edit().putString("assistant_name", name).apply()
        _settings.value = _settings.value.copy(assistantName = name)
    }

    fun updateWakePhrase(phrase: String) {
        prefs.edit().putString("wake_phrase", phrase).apply()
        _settings.value = _settings.value.copy(wakePhrase = phrase)
    }

    fun updateLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _settings.value = _settings.value.copy(language = lang)
    }

    fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat("speech_rate", rate).apply()
        _settings.value = _settings.value.copy(speechRate = rate)
    }

    fun updatePitch(pitch: Float) {
        prefs.edit().putFloat("pitch", pitch).apply()
        _settings.value = _settings.value.copy(pitch = pitch)
    }

    fun updateBackgroundListening(enabled: Boolean) {
        prefs.edit().putBoolean("bg_listening", enabled).apply()
        _settings.value = _settings.value.copy(backgroundListening = enabled)
    }

    fun updateOutfit(outfit: CharacterOutfit) {
        prefs.edit().putString("outfit", outfit.id).apply()
        _settings.value = _settings.value.copy(selectedOutfit = outfit)
    }

    fun updateTheme(theme: AssistantTheme) {
        prefs.edit().putString("theme", theme.id).apply()
        _settings.value = _settings.value.copy(selectedTheme = theme)
    }

    fun updateAutoSpeak(autoSpeak: Boolean) {
        prefs.edit().putBoolean("auto_speak", autoSpeak).apply()
        _settings.value = _settings.value.copy(autoSpeak = autoSpeak)
    }

    fun updateVoiceEnrollment(enrolled: Boolean, userName: String = "Master") {
        prefs.edit()
            .putBoolean("voice_enrolled", enrolled)
            .putString("enrolled_user_name", userName)
            .apply()
        _settings.value = _settings.value.copy(voiceEnrolled = enrolled, enrolledUserName = userName)
    }

    fun clearAllAssistantData() {
        prefs.edit().clear().apply()
        _settings.value = loadSettings()
    }
}
