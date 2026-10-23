package com.example

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ConversationEngine
import com.example.ai.ParsedIntent
import com.example.automation.AppController
import com.example.data.PreferencesManager
import com.example.model.ActionCommand
import com.example.model.AssistantSettings
import com.example.model.AssistantTheme
import com.example.model.CharacterOutfit
import com.example.model.CharacterState
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.service.HeenaAccessibilityService
import com.example.service.HeenaAssistantVoiceService
import com.example.service.ScreenAnalysis
import com.example.speech.SpeechEventListener
import com.example.speech.SpeechManager
import com.example.speech.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application), SpeechEventListener {

    private val preferencesManager = PreferencesManager(application)
    val settings: StateFlow<AssistantSettings> = preferencesManager.settings

    val apiKeyManager = com.example.api.ApiKeyManager(application)
    val securityManager = com.example.security.SecurityManager(application)
    val permissionManager = com.example.permissions.PermissionManager(application)
    val integrationManager = com.example.integrations.AppIntegrationManager(application)

    private val appController = AppController(application)
    private val conversationEngine = ConversationEngine(application, apiKeyManager)

    private val _characterState = MutableStateFlow(CharacterState.IDLE)
    val characterState: StateFlow<CharacterState> = _characterState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    val screenAnalysis: StateFlow<ScreenAnalysis?> = HeenaAccessibilityService.lastScreenAnalysis
    val isAccessibilityActive: StateFlow<Boolean> = HeenaAccessibilityService.isServiceActive
    val isAutomationPaused: StateFlow<Boolean> = HeenaAccessibilityService.isAutomationPaused

    private var ttsManager: TtsManager? = null
    private var speechManager: SpeechManager? = null

    private var pendingActionCommand: ActionCommand? = null

    init {
        initTts()
        initSpeech()
        checkPermissions()

        // Initial welcome message
        val welcomeMsg = ChatMessage(
            sender = MessageSender.HEENA,
            text = "नमस्ते! मैं ${settings.value.assistantName} हूँ! 🌸 'Hello Heena' बोलें या माइक दबाएं। (Say 'Hello Heena' or tap the mic!)"
        )
        _messages.value = listOf(welcomeMsg)
    }

    private fun initTts() {
        ttsManager = TtsManager(
            context = getApplication(),
            onSpeechStart = {
                _characterState.value = CharacterState.SPEAKING
            },
            onSpeechDone = {
                if (_characterState.value == CharacterState.SPEAKING) {
                    _characterState.value = CharacterState.IDLE
                }
            }
        )
        ttsManager?.updateVoiceSettings(
            pitch = settings.value.pitch,
            rate = settings.value.speechRate,
            language = settings.value.language
        )
    }

    private fun initSpeech() {
        speechManager = SpeechManager(getApplication(), this)
        speechManager?.updateConfig(
            wakePhrase = settings.value.wakePhrase,
            language = settings.value.language
        )
    }

    fun checkPermissions() {
        val micGranted = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // Update settings representation
        preferencesManager.updateBackgroundListening(settings.value.backgroundListening)
    }

    // SpeechEventListener Implementation
    override fun onWakeWordDetected(phrase: String) {
        viewModelScope.launch {
            _characterState.value = CharacterState.LISTENING
            val feedback = "हाँ जी! मैं सुन रही हूँ। (Yes, I'm listening!)"
            addMessage(MessageSender.HEENA, feedback)
            if (settings.value.autoSpeak) {
                ttsManager?.speak(feedback)
            }
            delay(1000)
            speechManager?.startListeningForCommand()
        }
    }

    override fun onCommandReceived(command: String) {
        viewModelScope.launch {
            _isListening.value = false
            _partialTranscript.value = ""
            addMessage(MessageSender.USER, command)
            processUserCommand(command)
        }
    }

    override fun onPartialTranscription(partialText: String) {
        _partialTranscript.value = partialText
    }

    override fun onAudioLevelChanged(rmsDb: Float) {
        _audioLevel.value = rmsDb
    }

    override fun onListeningStateChanged(isListening: Boolean) {
        _isListening.value = isListening
        if (isListening) {
            _characterState.value = CharacterState.LISTENING
        } else if (_characterState.value == CharacterState.LISTENING) {
            _characterState.value = CharacterState.IDLE
        }
    }

    override fun onErrorOccurred(errorMessage: String) {
        _isListening.value = false
        _audioLevel.value = 0f
        if (_characterState.value == CharacterState.LISTENING) {
            _characterState.value = CharacterState.IDLE
        }
        addMessage(MessageSender.SYSTEM, errorMessage)
    }

    fun toggleListening() {
        if (_isListening.value) {
            speechManager?.stopListening()
            _characterState.value = CharacterState.IDLE
        } else {
            ttsManager?.stop()
            _characterState.value = CharacterState.LISTENING
            speechManager?.startListeningForCommand()
        }
    }

    fun sendTextQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            addMessage(MessageSender.USER, query)
            processUserCommand(query)
        }
    }

    private suspend fun processUserCommand(query: String) {
        _characterState.value = CharacterState.THINKING

        val parsedIntent = conversationEngine.processQuery(query, settings.value.assistantName)

        when (parsedIntent) {
            is ParsedIntent.DirectAction -> {
                executeDirectAction(parsedIntent.command, parsedIntent.responseSpeech)
            }
            is ParsedIntent.ConsequentialConfirmation -> {
                pendingActionCommand = parsedIntent.pendingCommand
                addMessage(
                    sender = MessageSender.HEENA,
                    text = parsedIntent.warning,
                    isPendingConfirmation = true
                )
                _characterState.value = CharacterState.SURPRISED
                if (settings.value.autoSpeak) {
                    ttsManager?.speak(parsedIntent.warning)
                }
            }
            is ParsedIntent.Conversational -> {
                addMessage(MessageSender.HEENA, parsedIntent.replyText)
                _characterState.value = CharacterState.HAPPY
                if (settings.value.autoSpeak) {
                    ttsManager?.speak(parsedIntent.replyText)
                }
            }
        }
    }

    private fun executeDirectAction(command: ActionCommand, speechFeedback: String) {
        var actionTag: String? = null
        when (command) {
            is ActionCommand.LaunchApp -> {
                val result = appController.launchAppByName(command.appName)
                actionTag = "App: ${result.targetApp ?: command.appName}"
                addMessage(MessageSender.HEENA, result.feedbackMessage, actionTag = actionTag)
                _characterState.value = if (result.success) CharacterState.HAPPY else CharacterState.ERROR
                if (settings.value.autoSpeak) ttsManager?.speak(result.feedbackMessage)
            }
            is ActionCommand.SearchYouTube -> {
                val result = appController.searchYouTube(command.query)
                actionTag = "YouTube Search"
                addMessage(MessageSender.HEENA, result.feedbackMessage, actionTag = actionTag)
                _characterState.value = CharacterState.HAPPY
                if (settings.value.autoSpeak) ttsManager?.speak(result.feedbackMessage)
            }
            is ActionCommand.SearchWeb -> {
                val result = appController.searchWeb(command.query)
                actionTag = "Google Search"
                addMessage(MessageSender.HEENA, result.feedbackMessage, actionTag = actionTag)
                _characterState.value = CharacterState.HAPPY
                if (settings.value.autoSpeak) ttsManager?.speak(result.feedbackMessage)
            }
            is ActionCommand.ScreenUnderstanding -> {
                val analysis = HeenaAccessibilityService.analyzeCurrentScreen()
                actionTag = "Screen Understanding"
                val reply = if (analysis != null) {
                    analysis.summaryHinglish
                } else {
                    "स्क्रीन का विश्लेषण करने के लिए Accessibility Service चालू करें। (Please enable Accessibility Service in Settings)"
                }
                addMessage(MessageSender.HEENA, reply, actionTag = actionTag)
                _characterState.value = CharacterState.CARING
                if (settings.value.autoSpeak) ttsManager?.speak(reply)
            }
            is ActionCommand.ScrollScreen -> {
                val forward = command.direction == "down"
                val scrolled = HeenaAccessibilityService.scrollScreen(forward)
                val reply = if (scrolled) {
                    "स्क्रीन स्क्रॉल कर दी है। (Scrolled screen ${command.direction})"
                } else {
                    "स्क्रीन स्क्रॉल करने के लिए Accessibility Service चालू करें।"
                }
                actionTag = "Scroll ${command.direction}"
                addMessage(MessageSender.HEENA, reply, actionTag = actionTag)
                _characterState.value = if (scrolled) CharacterState.HAPPY else CharacterState.ERROR
                if (settings.value.autoSpeak) ttsManager?.speak(reply)
            }
            is ActionCommand.SendMessage -> {
                val result = appController.draftWhatsAppMessage(command.recipient, command.messageText)
                actionTag = "Message Draft"
                addMessage(MessageSender.HEENA, result.feedbackMessage, actionTag = actionTag)
                _characterState.value = CharacterState.HAPPY
                if (settings.value.autoSpeak) ttsManager?.speak(result.feedbackMessage)
            }
            else -> {
                addMessage(MessageSender.HEENA, speechFeedback)
                _characterState.value = CharacterState.HAPPY
                if (settings.value.autoSpeak) ttsManager?.speak(speechFeedback)
            }
        }
    }

    fun confirmPendingAction(message: ChatMessage) {
        val cmd = pendingActionCommand ?: return
        pendingActionCommand = null

        // Mark message as confirmed in chat
        _messages.value = _messages.value.map {
            if (it.id == message.id) it.copy(confirmed = true, isPendingConfirmation = false) else it
        }

        when (cmd) {
            is ActionCommand.SendMessage -> {
                val result = appController.draftWhatsAppMessage(cmd.recipient, cmd.messageText)
                addMessage(MessageSender.HEENA, result.feedbackMessage, actionTag = "Authorized")
                if (settings.value.autoSpeak) ttsManager?.speak(result.feedbackMessage)
            }
            is ActionCommand.SystemNotice -> {
                resetAllAssistantData()
                addMessage(MessageSender.HEENA, "सारा डेटा मिटा दिया गया है। (Data wiped).")
            }
            else -> {
                executeDirectAction(cmd, "कार्रवाई पूरी कर दी गई है।")
            }
        }
    }

    fun cancelPendingAction(message: ChatMessage) {
        pendingActionCommand = null
        _messages.value = _messages.value.map {
            if (it.id == message.id) it.copy(isPendingConfirmation = false) else it
        }
        val cancelMsg = "कार्रवाई रद्द कर दी गई। (Action cancelled)."
        addMessage(MessageSender.HEENA, cancelMsg)
        _characterState.value = CharacterState.IDLE
        if (settings.value.autoSpeak) ttsManager?.speak(cancelMsg)
    }

    fun analyzeScreen() {
        val analysis = HeenaAccessibilityService.analyzeCurrentScreen()
        val text = analysis?.summaryHinglish ?: "स्क्रीन की जानकारी प्राप्त करने के लिए Accessibility Service ऑन करें।"
        addMessage(MessageSender.HEENA, text, actionTag = "Screen Analysis")
        if (settings.value.autoSpeak) ttsManager?.speak(text)
    }

    fun scrollScreen(forward: Boolean) {
        val success = HeenaAccessibilityService.scrollScreen(forward)
        val text = if (success) "स्क्रीन स्क्रॉल कर दी गई।" else "स्क्रॉल करने के लिए एक्सेसिबिलिटी सर्विस सक्षम करें।"
        addMessage(MessageSender.HEENA, text)
        if (settings.value.autoSpeak) ttsManager?.speak(text)
    }

    fun toggleAutomationPause() {
        val current = HeenaAccessibilityService.isAutomationPaused.value
        HeenaAccessibilityService.pauseAutomation(!current)
        val notice = if (!current) "ऑटोमेशन रोक दिया गया है (Automation Paused)." else "ऑटोमेशन फिर से चालू कर दिया गया है (Automation Resumed)."
        addMessage(MessageSender.SYSTEM, notice)
    }

    fun speakText(text: String) {
        ttsManager?.speak(text)
    }

    // Settings modifiers
    fun updateAssistantName(name: String) {
        preferencesManager.updateAssistantName(name)
    }

    fun updateWakePhrase(phrase: String) {
        preferencesManager.updateWakePhrase(phrase)
        speechManager?.updateConfig(phrase, settings.value.language)
    }

    fun updateLanguage(lang: String) {
        preferencesManager.updateLanguage(lang)
        speechManager?.updateConfig(settings.value.wakePhrase, lang)
        ttsManager?.updateVoiceSettings(settings.value.pitch, settings.value.speechRate, lang)
    }

    fun updateSpeechRate(rate: Float) {
        preferencesManager.updateSpeechRate(rate)
        ttsManager?.updateVoiceSettings(settings.value.pitch, rate, settings.value.language)
    }

    fun updatePitch(pitch: Float) {
        preferencesManager.updatePitch(pitch)
        ttsManager?.updateVoiceSettings(pitch, settings.value.speechRate, settings.value.language)
    }

    fun toggleAutoSpeak(autoSpeak: Boolean) {
        preferencesManager.updateAutoSpeak(autoSpeak)
    }

    fun toggleBackgroundListening(enabled: Boolean) {
        preferencesManager.updateBackgroundListening(enabled)
        val context = getApplication<Application>()
        if (enabled) {
            HeenaAssistantVoiceService.startService(context)
        } else {
            HeenaAssistantVoiceService.stopService(context)
        }
    }

    fun selectOutfit(outfit: CharacterOutfit) {
        preferencesManager.updateOutfit(outfit)
    }

    fun selectTheme(theme: AssistantTheme) {
        preferencesManager.updateTheme(theme)
    }

    fun enrollVoice(userName: String) {
        preferencesManager.updateVoiceEnrollment(true, userName)
        addMessage(MessageSender.SYSTEM, "Voice profile enrolled for '$userName'.")
    }

    fun clearVoiceData() {
        preferencesManager.updateVoiceEnrollment(false)
        addMessage(MessageSender.SYSTEM, "Voice profile data removed.")
    }

    fun resetAllAssistantData() {
        preferencesManager.clearAllAssistantData()
        apiKeyManager.removeApiKey()
        securityManager.removeAppLock()
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.HEENA,
                text = "नमस्ते! मैं ${settings.value.assistantName} हूँ! डेटा रीसेट हो गया है।"
            )
        )
    }

    private fun addMessage(
        sender: MessageSender,
        text: String,
        actionTag: String? = null,
        isPendingConfirmation: Boolean = false
    ) {
        val newMsg = ChatMessage(
            sender = sender,
            text = text,
            actionTag = actionTag,
            isPendingConfirmation = isPendingConfirmation
        )
        _messages.value = _messages.value + newMsg
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager?.shutdown()
    }
}
