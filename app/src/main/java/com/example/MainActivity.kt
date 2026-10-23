package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CharacterState
import com.example.ui.CharacterAvatar
import com.example.ui.ConversationView
import com.example.ui.ScreenUnderstandingSheet
import com.example.ui.SettingsScreen
import com.example.ui.theme.AnimeGlowCyan
import com.example.ui.theme.AnimeGlowPink
import com.example.ui.theme.AnimeGlowPurple
import com.example.ui.theme.AnimeListeningPulse
import com.example.ui.theme.HeenaAiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeenaAiTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf("main") }
    var showScreenSheet by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val characterState by viewModel.characterState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val screenAnalysis by viewModel.screenAnalysis.collectAsStateWithLifecycle()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsStateWithLifecycle()
    val isAutomationPaused by viewModel.isAutomationPaused.collectAsStateWithLifecycle()

    // Permission launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.checkPermissions()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val isAppLockEnabled by viewModel.securityManager.isAppLockEnabled.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.securityManager.isUnlocked.collectAsStateWithLifecycle()

    // App Lock Barrier
    if (isAppLockEnabled && !isAppUnlocked) {
        AppLockBarrierScreen(
            securityManager = viewModel.securityManager,
            assistantName = settings.assistantName
        )
        return
    }

    if (currentScreen == "settings") {
        SettingsScreen(
            settings = settings,
            apiKeyManager = viewModel.apiKeyManager,
            securityManager = viewModel.securityManager,
            permissionManager = viewModel.permissionManager,
            integrationManager = viewModel.integrationManager,
            isAccessibilityActive = isAccessibilityActive,
            onBack = { currentScreen = "main" },
            onUpdateName = { viewModel.updateAssistantName(it) },
            onUpdateWakePhrase = { viewModel.updateWakePhrase(it) },
            onUpdateLanguage = { viewModel.updateLanguage(it) },
            onUpdateSpeechRate = { viewModel.updateSpeechRate(it) },
            onUpdatePitch = { viewModel.updatePitch(it) },
            onToggleAutoSpeak = { viewModel.toggleAutoSpeak(it) },
            onToggleBackgroundListening = { viewModel.toggleBackgroundListening(it) },
            onSelectOutfit = { viewModel.selectOutfit(it) },
            onSelectTheme = { viewModel.selectTheme(it) },
            onRequestPermission = { perm ->
                permissionLauncher.launch(arrayOf(perm))
            },
            onEnrollVoice = { viewModel.enrollVoice(it) },
            onClearVoiceData = { viewModel.clearVoiceData() },
            onResetAllData = { viewModel.resetAllAssistantData() }
        )
    } else {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    assistantName = settings.assistantName,
                    isListening = isListening,
                    isAccessibilityActive = isAccessibilityActive,
                    onOpenScreenSheet = { showScreenSheet = true },
                    onOpenSettings = { currentScreen = "settings" }
                )
            },
            bottomBar = {
                MainBottomControls(
                    isListening = isListening,
                    audioLevel = audioLevel,
                    textInput = textInput,
                    onTextChange = { textInput = it },
                    onSendQuery = {
                        viewModel.sendTextQuery(textInput)
                        textInput = ""
                    },
                    onToggleMic = { viewModel.toggleListening() },
                    onOpenScreenSheet = { showScreenSheet = true }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Character Anime Hero Display
                CharacterAvatar(
                    characterState = characterState,
                    audioLevel = audioLevel,
                    isSpeaking = characterState == CharacterState.SPEAKING,
                    outfit = settings.selectedOutfit,
                    assistantName = settings.assistantName
                )

                // Live speech transcript preview banner
                AnimatedVisibility(
                    visible = isListening && partialTranscript.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AnimeListeningPulse.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = AnimeListeningPulse, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "\"$partialTranscript...\"",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Interactive Conversation List & Suggestion Chips
                ConversationView(
                    messages = messages,
                    onChipClicked = { viewModel.sendTextQuery(it) },
                    onConfirmAction = { viewModel.confirmPendingAction(it) },
                    onCancelAction = { viewModel.cancelPendingAction(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Screen Understanding Bottom Sheet
        if (showScreenSheet) {
            ScreenUnderstandingSheet(
                sheetState = sheetState,
                isServiceActive = isAccessibilityActive,
                analysis = screenAnalysis,
                isAutomationPaused = isAutomationPaused,
                onDismiss = { showScreenSheet = false },
                onScrollDown = { viewModel.scrollScreen(true) },
                onScrollUp = { viewModel.scrollScreen(false) },
                onReadAloud = { viewModel.speakText(it) },
                onTogglePause = { viewModel.toggleAutomationPause() }
            )
        }
    }
}

@Composable
fun MainTopAppBar(
    assistantName: String,
    isListening: Boolean,
    isAccessibilityActive: Boolean,
    onOpenScreenSheet: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding & Privacy Mic Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AnimeGlowPurple, AnimeGlowPink)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌸", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = assistantName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Native AI Voice Assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right Action Controls
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Microphone Privacy Dot
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Screen Assistant Shortcut
                IconButton(
                    onClick = onOpenScreenSheet,
                    modifier = Modifier.testTag("top_screen_analysis_btn")
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = "Screen Understanding",
                        tint = if (isAccessibilityActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Settings & Privacy
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("top_settings_btn")
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings & Privacy",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MainBottomControls(
    isListening: Boolean,
    audioLevel: Float,
    textInput: String,
    onTextChange: (String) -> Unit,
    onSendQuery: () -> Unit,
    onToggleMic: () -> Unit,
    onOpenScreenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_ripple")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Text Input Field & Send Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextChange,
                    placeholder = { Text("Ask Heena or type a command...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_text_input")
                )

                if (textInput.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSendQuery,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("send_query_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Big Center Anime Voice Mic Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                // Expanding wave ripple ring when listening
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                AnimeListeningPulse.copy(alpha = 0.35f)
                            )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isListening) AnimeListeningPulse else MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable { onToggleMic() }
                        .testTag("mic_toggle_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (isListening) "Listening" else "Tap to Speak",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Text(
                text = if (isListening) "Listening... say a command or 'Hello Heena'" else "Tap mic or say 'Hello Heena'",
                style = MaterialTheme.typography.labelSmall,
                color = if (isListening) AnimeListeningPulse else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppLockBarrierScreen(
    securityManager: com.example.security.SecurityManager,
    assistantName: String,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$assistantName is Locked 🔒",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your in-app security PIN to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = enteredPin,
                onValueChange = {
                    if (it.length <= 6) {
                        enteredPin = it
                        pinError = null
                    }
                },
                label = { Text("Security PIN") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .testTag("app_lock_pin_input")
            )

            if (pinError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pinError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (securityManager.verifyPin(enteredPin)) {
                        enteredPin = ""
                        pinError = null
                    } else {
                        pinError = "Incorrect PIN. Try again."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .testTag("unlock_app_btn")
            ) {
                Text("Unlock")
            }

            if (securityManager.isBiometricEnabled.value) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        // Unlocks session
                        securityManager.unlockViaBiometric()
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Biometric Unlock")
                }
            }
        }
    }
}

