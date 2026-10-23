package com.example.model

data class AssistantSettings(
    val assistantName: String = "Heena AI",
    val wakePhrase: String = "Hello Heena",
    val language: String = "Hinglish", // "Hindi", "English", "Hinglish"
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.15f,
    val backgroundListening: Boolean = false,
    val selectedOutfit: CharacterOutfit = CharacterOutfit.CYBER_TECH,
    val selectedTheme: AssistantTheme = AssistantTheme.CYBER_NEON,
    val autoSpeak: Boolean = true,
    val voiceEnrolled: Boolean = false,
    val enrolledUserName: String = "Master",
    val accessibilityEnabled: Boolean = false,
    val micPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false
)
