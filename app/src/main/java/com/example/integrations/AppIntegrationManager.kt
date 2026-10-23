package com.example.integrations

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class IntegrationApp(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val isEnabled: Boolean,
    val intentMechanism: String,
    val requiredPermission: String
)

class AppIntegrationManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("heena_integrations_prefs", Context.MODE_PRIVATE)

    private val _isGoogleConnected = MutableStateFlow(prefs.getBoolean("google_connected", false))
    val isGoogleConnected: StateFlow<Boolean> = _isGoogleConnected.asStateFlow()

    private val _connectedEmail = MutableStateFlow(prefs.getString("google_email", null))
    val connectedEmail: StateFlow<String?> = _connectedEmail.asStateFlow()

    private val _integrations = MutableStateFlow(loadIntegrations())
    val integrations: StateFlow<List<IntegrationApp>> = _integrations.asStateFlow()

    private fun loadIntegrations(): List<IntegrationApp> {
        return listOf(
            IntegrationApp(
                id = "youtube",
                name = "YouTube",
                description = "Open YouTube and search videos/music using voice commands like 'YouTube par song search karo'.",
                iconEmoji = "🎬",
                isEnabled = prefs.getBoolean("integration_youtube", true),
                intentMechanism = "Android Search Intent (com.google.android.youtube)",
                requiredPermission = "None (Standard Launch Intent)"
            ),
            IntegrationApp(
                id = "whatsapp",
                name = "WhatsApp",
                description = "Draft messages for contacts with explicit confirmation before sending.",
                iconEmoji = "💬",
                isEnabled = prefs.getBoolean("integration_whatsapp", true),
                intentMechanism = "Official Send Intent (https://api.whatsapp.com/send)",
                requiredPermission = "User Action Confirmation Required"
            ),
            IntegrationApp(
                id = "chrome",
                name = "Google Chrome & Search",
                description = "Perform Google web searches and open links.",
                iconEmoji = "🌐",
                isEnabled = prefs.getBoolean("integration_chrome", true),
                intentMechanism = "Android Web Search Intent (ACTION_WEB_SEARCH)",
                requiredPermission = "INTERNET"
            ),
            IntegrationApp(
                id = "gmail",
                name = "Gmail & Email",
                description = "Compose and draft emails using official Android mailto intent.",
                iconEmoji = "✉️",
                isEnabled = prefs.getBoolean("integration_gmail", true),
                intentMechanism = "ACTION_SENDTO (mailto:)",
                requiredPermission = "User Action Confirmation Required"
            ),
            IntegrationApp(
                id = "maps",
                name = "Google Maps",
                description = "Look up places, routes, and directions via voice.",
                iconEmoji = "🗺️",
                isEnabled = prefs.getBoolean("integration_maps", true),
                intentMechanism = "geo: or Maps Search Intent",
                requiredPermission = "INTERNET"
            )
        )
    }

    fun toggleIntegration(id: String, enabled: Boolean) {
        prefs.edit().putBoolean("integration_$id", enabled).apply()
        _integrations.value = loadIntegrations()
    }

    fun connectGoogleAccount(email: String) {
        prefs.edit()
            .putBoolean("google_connected", true)
            .putString("google_email", email)
            .apply()
        _isGoogleConnected.value = true
        _connectedEmail.value = email
    }

    fun disconnectGoogleAccount() {
        prefs.edit()
            .putBoolean("google_connected", false)
            .remove("google_email")
            .apply()
        _isGoogleConnected.value = false
        _connectedEmail.value = null
    }
}
