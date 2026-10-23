package com.example.model

enum class CharacterState(
    val title: String,
    val description: String,
    val moodEmoji: String
) {
    IDLE("Ready & Online", "Heena is waiting for your command", "✨"),
    LISTENING("Listening...", "Say a command or ask a question", "🎧"),
    THINKING("Thinking...", "Processing with AI logic", "💭"),
    SPEAKING("Speaking...", "Answering with voice synthesis", "💬"),
    HAPPY("Happy!", "Action executed successfully", "💖"),
    SURPRISED("Surprised", "New context or exciting action", "⭐"),
    CARING("Helpful", "Guiding you through steps", "🌸"),
    ERROR("Oops!", "Encountered an issue or restriction", "⚠️")
}

enum class CharacterOutfit(
    val id: String,
    val displayName: String,
    val description: String
) {
    CYBER_TECH("cyber_tech", "Cyber Tech", "Futuristic headset and neon jacket"),
    SAKURA_UNIFORM("sakura_uniform", "Sakura Uniform", "School girl aesthetic with ribbon"),
    MAGICAL_IDOL("magical_idol", "Magical Idol", "Pastel sparkles and star headphones"),
    TRADITIONAL_KIMONO("traditional_kimono", "Yukata Kimono", "Graceful traditional Japanese festive outfit")
}

enum class AssistantTheme(
    val id: String,
    val displayName: String,
    val primaryHex: Long
) {
    CYBER_NEON("cyber_neon", "Cyber Neon", 0xFF8B5CF6),
    SAKURA_PINK("sakura_pink", "Sakura Blossom", 0xFFEC4899),
    ROYAL_VIOLET("royal_violet", "Royal Violet", 0xFF6366F1),
    SUNSET_AMBER("sunset_amber", "Sunset Amber", 0xFFF59E0B)
}
