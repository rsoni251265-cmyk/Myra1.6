package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.ApiKeyManager
import com.example.integrations.AppIntegrationManager
import com.example.model.AssistantSettings
import com.example.model.AssistantTheme
import com.example.model.CharacterOutfit
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionState
import com.example.security.SecurityManager
import com.example.service.HeenaAssistantVoiceService
import com.example.ui.settings.ApiKeySettingsScreen
import com.example.ui.settings.IntegrationsSettingsScreen
import com.example.ui.settings.PermissionsSettingsScreen
import com.example.ui.settings.SecuritySettingsScreen

enum class SettingsSubPage {
    HUB,
    API_KEY,
    PERMISSIONS,
    SECURITY,
    INTEGRATIONS,
    VOICE,
    BACKGROUND,
    THEMES,
    PRIVACY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AssistantSettings,
    apiKeyManager: ApiKeyManager,
    securityManager: SecurityManager,
    permissionManager: PermissionManager,
    integrationManager: AppIntegrationManager,
    isAccessibilityActive: Boolean,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateWakePhrase: (String) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateSpeechRate: (Float) -> Unit,
    onUpdatePitch: (Float) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onToggleBackgroundListening: (Boolean) -> Unit,
    onSelectOutfit: (CharacterOutfit) -> Unit,
    onSelectTheme: (AssistantTheme) -> Unit,
    onRequestPermission: (String) -> Unit,
    onEnrollVoice: (String) -> Unit,
    onClearVoiceData: () -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSubPage by remember { mutableStateOf(SettingsSubPage.HUB) }
    var showAuthPromptForPage by remember { mutableStateOf<SettingsSubPage?>(null) }
    var pinAuthInput by remember { mutableStateOf("") }
    var pinAuthError by remember { mutableStateOf<String?>(null) }

    // Intercept navigation for sensitive pages if security protection is on
    val navigateToPage: (SettingsSubPage) -> Unit = { page ->
        if ((page == SettingsSubPage.API_KEY || page == SettingsSubPage.SECURITY) &&
            securityManager.isAppLockEnabled.value &&
            securityManager.protectSensitiveSettings.value &&
            securityManager.hasPin() &&
            !securityManager.isUnlocked.value
        ) {
            showAuthPromptForPage = page
            pinAuthInput = ""
            pinAuthError = null
        } else {
            currentSubPage = page
        }
    }

    when (currentSubPage) {
        SettingsSubPage.API_KEY -> {
            ApiKeySettingsScreen(
                apiKeyManager = apiKeyManager,
                onBack = { currentSubPage = SettingsSubPage.HUB }
            )
        }
        SettingsSubPage.PERMISSIONS -> {
            PermissionsSettingsScreen(
                permissionManager = permissionManager,
                onRequestPermission = onRequestPermission,
                onBack = { currentSubPage = SettingsSubPage.HUB }
            )
        }
        SettingsSubPage.SECURITY -> {
            SecuritySettingsScreen(
                securityManager = securityManager,
                onBack = { currentSubPage = SettingsSubPage.HUB }
            )
        }
        SettingsSubPage.INTEGRATIONS -> {
            IntegrationsSettingsScreen(
                integrationManager = integrationManager,
                onBack = { currentSubPage = SettingsSubPage.HUB }
            )
        }
        SettingsSubPage.VOICE -> {
            VoiceSettingsSubPage(
                settings = settings,
                onBack = { currentSubPage = SettingsSubPage.HUB },
                onUpdateName = onUpdateName,
                onUpdateWakePhrase = onUpdateWakePhrase,
                onUpdateLanguage = onUpdateLanguage,
                onUpdateSpeechRate = onUpdateSpeechRate,
                onUpdatePitch = onUpdatePitch,
                onToggleAutoSpeak = onToggleAutoSpeak
            )
        }
        SettingsSubPage.BACKGROUND -> {
            BackgroundServiceSubPage(
                settings = settings,
                onBack = { currentSubPage = SettingsSubPage.HUB },
                onToggleBackgroundListening = onToggleBackgroundListening
            )
        }
        SettingsSubPage.THEMES -> {
            ThemeCharacterSubPage(
                settings = settings,
                onBack = { currentSubPage = SettingsSubPage.HUB },
                onSelectOutfit = onSelectOutfit,
                onSelectTheme = onSelectTheme
            )
        }
        SettingsSubPage.PRIVACY -> {
            PrivacyDataSubPage(
                settings = settings,
                onBack = { currentSubPage = SettingsSubPage.HUB },
                onEnrollVoice = onEnrollVoice,
                onClearVoiceData = onClearVoiceData,
                onResetAllData = onResetAllData
            )
        }
        SettingsSubPage.HUB -> {
            SettingsHubView(
                settings = settings,
                apiKeyManager = apiKeyManager,
                securityManager = securityManager,
                permissionManager = permissionManager,
                integrationManager = integrationManager,
                onBack = onBack,
                onNavigate = navigateToPage,
                modifier = modifier
            )
        }
    }

    // In-App Re-Authentication Prompt Dialog
    if (showAuthPromptForPage != null) {
        AlertDialog(
            onDismissRequest = {
                showAuthPromptForPage = null
                pinAuthInput = ""
                pinAuthError = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Security Authentication")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your 4-digit in-app PIN to access sensitive settings:")
                    OutlinedTextField(
                        value = pinAuthInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                pinAuthInput = it
                                pinAuthError = null
                            }
                        },
                        label = { Text("In-App PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("pin_auth_input")
                    )

                    AnimatedVisibility(visible = pinAuthError != null) {
                        Text(pinAuthError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (securityManager.verifyPin(pinAuthInput)) {
                            val target = showAuthPromptForPage!!
                            showAuthPromptForPage = null
                            pinAuthInput = ""
                            pinAuthError = null
                            currentSubPage = target
                        } else {
                            pinAuthError = "Incorrect PIN"
                        }
                    },
                    modifier = Modifier.testTag("submit_pin_btn")
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAuthPromptForPage = null
                    pinAuthInput = ""
                    pinAuthError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubView(
    settings: AssistantSettings,
    apiKeyManager: ApiKeyManager,
    securityManager: SecurityManager,
    permissionManager: PermissionManager,
    integrationManager: AppIntegrationManager,
    onBack: () -> Unit,
    onNavigate: (SettingsSubPage) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val scrollState = rememberScrollState()

    val permissionsList = remember { permissionManager.getPermissionsList() }
    val allowedCount = permissionsList.count { it.currentStatus == com.example.permissions.PermissionState.ALLOWED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Hero Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Heena AI Control Center", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "Manage API keys, app permissions, security locks, and assistant preferences.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text("Core Configuration", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))

            // 1. API Key Manager
            SettingsHubCard(
                icon = Icons.Default.Key,
                title = "API Key Manager",
                subtitle = "Configure Google Gemini API or custom endpoints",
                statusLabel = if (apiKeyManager.hasKey.value) "${apiKeyManager.model.value} (Saved)" else "Not Configured",
                statusColor = if (apiKeyManager.hasKey.value) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                statusBg = if (apiKeyManager.hasKey.value) Color(0xFF22C55E).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                onClick = { onNavigate(SettingsSubPage.API_KEY) },
                testTag = "hub_api_key_card"
            )

            // 2. App Permissions Manager
            SettingsHubCard(
                icon = Icons.Default.Security,
                title = "App Permissions Manager",
                subtitle = "Microphone, Camera, Notifications & Accessibility",
                statusLabel = "$allowedCount of ${permissionsList.size} Active",
                statusColor = if (allowedCount >= 4) Color(0xFF15803D) else Color(0xFFB45309),
                statusBg = if (allowedCount >= 4) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                onClick = { onNavigate(SettingsSubPage.PERMISSIONS) },
                testTag = "hub_permissions_card"
            )

            // 3. Password & Security Settings
            SettingsHubCard(
                icon = Icons.Default.Lock,
                title = "Password & Security Settings",
                subtitle = "In-App PIN lock, biometrics, and auto-lock timeout",
                statusLabel = if (securityManager.isAppLockEnabled.value) "PIN Protected" else "Disabled",
                statusColor = if (securityManager.isAppLockEnabled.value) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                statusBg = if (securityManager.isAppLockEnabled.value) Color(0xFF22C55E).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                onClick = { onNavigate(SettingsSubPage.SECURITY) },
                testTag = "hub_security_card"
            )

            // 4. Google Account & App Integrations
            SettingsHubCard(
                icon = Icons.Default.AccountCircle,
                title = "Google & App Integrations",
                subtitle = "YouTube, WhatsApp, Gmail, Chrome, and Maps access",
                statusLabel = if (integrationManager.isGoogleConnected.value) "Google Linked" else "5 Apps Supported",
                statusColor = MaterialTheme.colorScheme.primary,
                statusBg = MaterialTheme.colorScheme.primaryContainer,
                onClick = { onNavigate(SettingsSubPage.INTEGRATIONS) },
                testTag = "hub_integrations_card"
            )

            Text("Assistant Customization", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))

            // 5. Voice Assistant Settings
            SettingsHubCard(
                icon = Icons.Default.VolumeUp,
                title = "Voice Assistant Settings",
                subtitle = "Wake phrase, language, speech speed, and pitch",
                statusLabel = "\"${settings.wakePhrase}\" • ${settings.language}",
                statusColor = MaterialTheme.colorScheme.primary,
                statusBg = MaterialTheme.colorScheme.primaryContainer,
                onClick = { onNavigate(SettingsSubPage.VOICE) },
                testTag = "hub_voice_card"
            )

            // 6. Background Service Settings
            SettingsHubCard(
                icon = Icons.Default.Mic,
                title = "Background Service Settings",
                subtitle = "Continuous wake-word listening & battery optimization",
                statusLabel = if (HeenaAssistantVoiceService.isRunning.value) "Listening Active" else "Stopped",
                statusColor = if (HeenaAssistantVoiceService.isRunning.value) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                statusBg = if (HeenaAssistantVoiceService.isRunning.value) Color(0xFF22C55E).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                onClick = { onNavigate(SettingsSubPage.BACKGROUND) },
                testTag = "hub_background_card"
            )

            // 7. Theme & Anime Character
            SettingsHubCard(
                icon = Icons.Default.Palette,
                title = "Theme & Anime Character",
                subtitle = "Character outfits, appearance, and vibrant color themes",
                statusLabel = "${settings.selectedOutfit.displayName} • ${settings.selectedTheme.displayName}",
                statusColor = MaterialTheme.colorScheme.primary,
                statusBg = MaterialTheme.colorScheme.primaryContainer,
                onClick = { onNavigate(SettingsSubPage.THEMES) },
                testTag = "hub_themes_card"
            )

            // 8. Privacy & Data Controls
            SettingsHubCard(
                icon = Icons.Default.Delete,
                title = "Privacy & Data Controls",
                subtitle = "Voice enrollment profile and local data wipe",
                statusLabel = if (settings.voiceEnrolled) "Voice Enrolled" else "Local Storage Only",
                statusColor = MaterialTheme.colorScheme.primary,
                statusBg = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { onNavigate(SettingsSubPage.PRIVACY) },
                testTag = "hub_privacy_card"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsHubCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusLabel: String,
    statusColor: Color,
    statusBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Sub-page: Voice Settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsSubPage(
    settings: AssistantSettings,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateWakePhrase: (String) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateSpeechRate: (Float) -> Unit,
    onUpdatePitch: (Float) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit
) {
    BackHandler { onBack() }
    val scrollState = rememberScrollState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showWakeWordDialog by remember { mutableStateOf(false) }
    var tempAssistantName by remember { mutableStateOf(settings.assistantName) }
    var tempWakeWord by remember { mutableStateOf(settings.wakePhrase) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Assistant Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showNameDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Assistant Name", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(settings.assistantName, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Edit", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showWakeWordDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wake Phrase", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("\"${settings.wakePhrase}\"", color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Edit", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Language Mode", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Hinglish", "Hindi", "English").forEach { lang ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { onUpdateLanguage(lang) },
                                label = { Text(lang) }
                            )
                        }
                    }

                    HorizontalDivider()

                    var rateSlider by remember { mutableFloatStateOf(settings.speechRate) }
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Speech Speed")
                            Text(String.format("%.1fx", rateSlider))
                        }
                        Slider(
                            value = rateSlider,
                            onValueChange = {
                                rateSlider = it
                                onUpdateSpeechRate(it)
                            },
                            valueRange = 0.7f..1.4f,
                            steps = 6
                        )
                    }

                    var pitchSlider by remember { mutableFloatStateOf(settings.pitch) }
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Anime Voice Pitch")
                            Text(String.format("%.2f", pitchSlider))
                        }
                        Slider(
                            value = pitchSlider,
                            onValueChange = {
                                pitchSlider = it
                                onUpdatePitch(it)
                            },
                            valueRange = 0.8f..1.5f,
                            steps = 7
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Speak Responses", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Speak responses aloud via Text-to-Speech", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.autoSpeak,
                            onCheckedChange = onToggleAutoSpeak
                        )
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Change Assistant Name") },
            text = {
                OutlinedTextField(
                    value = tempAssistantName,
                    onValueChange = { tempAssistantName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempAssistantName.isNotBlank()) onUpdateName(tempAssistantName.trim())
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showWakeWordDialog) {
        AlertDialog(
            onDismissRequest = { showWakeWordDialog = false },
            title = { Text("Change Wake Phrase") },
            text = {
                OutlinedTextField(
                    value = tempWakeWord,
                    onValueChange = { tempWakeWord = it },
                    label = { Text("Wake Phrase") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempWakeWord.isNotBlank()) onUpdateWakePhrase(tempWakeWord.trim())
                    showWakeWordDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showWakeWordDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Sub-page: Background Service Settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundServiceSubPage(
    settings: AssistantSettings,
    onBack: () -> Unit,
    onToggleBackgroundListening: (Boolean) -> Unit
) {
    BackHandler { onBack() }
    val isRunning = HeenaAssistantVoiceService.isRunning.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Background Service", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Background Wake Word", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Heena listens for \"${settings.wakePhrase}\" even when another application is active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.backgroundListening,
                            onCheckedChange = onToggleBackgroundListening
                        )
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isRunning) Icons.Default.Check else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isRunning) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isRunning) "Active Notification Banner: \"Heena AI is Active 🎧\"" else "Service Stopped",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Android Battery Optimization", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "• Android compliant foreground service with low-power microphone listening.\n" +
                                "• To prevent Android from pausing background listening, allow 'Unrestricted' battery in Device App Info if needed.\n" +
                                "• Heena AI never secretly records audio and always displays an ongoing notification while listening.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
                    )
                }
            }
        }
    }
}

// Sub-page: Theme & Character Settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCharacterSubPage(
    settings: AssistantSettings,
    onBack: () -> Unit,
    onSelectOutfit: (CharacterOutfit) -> Unit,
    onSelectTheme: (AssistantTheme) -> Unit
) {
    BackHandler { onBack() }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme & Anime Character", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Character Outfit Style", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CharacterOutfit.values().forEach { outfit ->
                            FilterChip(
                                selected = settings.selectedOutfit == outfit,
                                onClick = { onSelectOutfit(outfit) },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(outfit.displayName, fontWeight = FontWeight.Bold)
                                        Text(outfit.description, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Interface Theme Palette", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistantTheme.values().forEach { theme ->
                            FilterChip(
                                selected = settings.selectedTheme == theme,
                                onClick = { onSelectTheme(theme) },
                                label = { Text(theme.displayName, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sub-page: Privacy & Data Controls
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataSubPage(
    settings: AssistantSettings,
    onBack: () -> Unit,
    onEnrollVoice: (String) -> Unit,
    onClearVoiceData: () -> Unit,
    onResetAllData: () -> Unit
) {
    BackHandler { onBack() }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Data Controls", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Personal Voice Recognition Profile", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        if (settings.voiceEnrolled) "Voice profile enrolled as: ${settings.enrolledUserName}" else "Voice enrollment is opt-in. Stored templates are strictly on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (settings.voiceEnrolled) {
                            OutlinedButton(onClick = onClearVoiceData) { Text("Delete Voice Template") }
                        } else {
                            Button(onClick = { onEnrollVoice("Master") }) { Text("Enroll Voice Sample") }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Wipe All Local Assistant Data", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                    Text(
                        "Completely deletes all local preferences, voice profiles, conversation history, and resets Heena AI to initial factory state.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("wipe_all_data_btn")
                    ) {
                        Text("Reset All Data")
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirm Data Reset?") },
            text = { Text("This will permanently remove all settings, voice enrollment, and stored API keys.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
