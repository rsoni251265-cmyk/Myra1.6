package com.example.model

enum class MessageSender {
    USER,
    HEENA,
    SYSTEM
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionTag: String? = null,
    val isPendingConfirmation: Boolean = false,
    val confirmed: Boolean = false,
    val executionResult: String? = null
)

sealed class ActionCommand {
    data class LaunchApp(val appName: String, val packageName: String, val displayName: String) : ActionCommand()
    data class SearchWeb(val query: String) : ActionCommand()
    data class SearchYouTube(val query: String) : ActionCommand()
    data class SendMessage(val platform: String, val recipient: String, val messageText: String) : ActionCommand()
    data class ScreenUnderstanding(val query: String) : ActionCommand()
    data class ScrollScreen(val direction: String) : ActionCommand()
    data class GeneralResponse(val text: String) : ActionCommand()
    data class ConsequentialConfirm(val description: String, val onConfirmCommand: ActionCommand) : ActionCommand()
    data class SystemNotice(val message: String) : ActionCommand()
}
