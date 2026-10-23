package com.example.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.ActionCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ParsedIntent {
    data class DirectAction(val command: ActionCommand, val responseSpeech: String) : ParsedIntent()
    data class ConsequentialConfirmation(val warning: String, val pendingCommand: ActionCommand) : ParsedIntent()
    data class Conversational(val replyText: String) : ParsedIntent()
}

class ConversationEngine(
    private val context: Context,
    private val apiKeyManager: com.example.api.ApiKeyManager? = null
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun processQuery(userInput: String, assistantName: String = "Heena AI"): ParsedIntent = withContext(Dispatchers.Default) {
        val clean = userInput.trim()
        val lower = clean.lowercase()

        // 1. Check for Consequential Destructive actions first
        if (lower.contains("delete all") || lower.contains("sab data delete karo") || lower.contains("factory reset") || lower.contains("clear all data")) {
            return@withContext ParsedIntent.ConsequentialConfirmation(
                warning = "क्या आप सच में सारा असिस्टेंट डेटा मिटाना चाहते हैं? (Are you sure you want to delete all assistant data?)",
                pendingCommand = ActionCommand.SystemNotice("Assistant data wiped successfully.")
            )
        }

        // 2. Check for Screen Understanding commands
        if (lower.contains("screen samjhao") || lower.contains("screen par kya") ||
            lower.contains("explain screen") || lower.contains("read screen") ||
            lower.contains("is text ko padhkar") || lower.contains("what is on the screen")) {
            return@withContext ParsedIntent.DirectAction(
                command = ActionCommand.ScreenUnderstanding(clean),
                responseSpeech = "स्क्रीन का विश्लेषण कर रही हूँ! (Analyzing current screen)"
            )
        }

        // 3. Check for Screen Scrolling
        if (lower.contains("scroll") || lower.contains("neeche karo") || lower.contains("upar karo")) {
            val direction = if (lower.contains("up") || lower.contains("upar")) "up" else "down"
            return@withContext ParsedIntent.DirectAction(
                command = ActionCommand.ScrollScreen(direction),
                responseSpeech = "स्क्रीन स्क्रॉल कर रही हूँ! (Scrolling screen $direction)"
            )
        }

        // 4. YouTube search
        val ytMatch = Regex("(?i)youtube\\s+(?:par|pe|mein)?\\s*(.+?)\\s*(?:search\\s*karo|chalao|play|dikhaye)").find(clean)
            ?: Regex("(?i)search\\s*(.+?)\\s*on\\s*youtube").find(clean)
            ?: Regex("(?i)play\\s*(.+?)\\s*on\\s*youtube").find(clean)
        if (ytMatch != null && ytMatch.groupValues.size > 1) {
            val query = ytMatch.groupValues[1].trim()
            return@withContext ParsedIntent.DirectAction(
                command = ActionCommand.SearchYouTube(query),
                responseSpeech = "YouTube पर '$query' खोज रही हूँ! (Searching YouTube for $query)"
            )
        }

        // 5. Web / Google search
        val searchMatch = Regex("(?i)google\\s+(?:par|pe)?\\s*(.+?)\\s*(?:search\\s*karo|dhoondo|batao)").find(clean)
            ?: Regex("(?i)search\\s*(.+?)\\s*on\\s*google").find(clean)
        if (searchMatch != null && searchMatch.groupValues.size > 1) {
            val query = searchMatch.groupValues[1].trim()
            return@withContext ParsedIntent.DirectAction(
                command = ActionCommand.SearchWeb(query),
                responseSpeech = "Google पर '$query' सर्च कर रही हूँ! (Searching Google)"
            )
        }

        // 6. Messaging / WhatsApp drafting
        val waMatch = Regex("(?i)whatsapp\\s+(?:par|pe)?\\s*([a-zA-Z0-9]+)?\\s*(?:ko)?\\s*(.+?)\\s*(?:message|likho|send|bhejo)").find(clean)
            ?: Regex("(?i)send\\s+message\\s+to\\s*([a-zA-Z0-9]+)?\\s*(?:saying)?\\s*(.+)").find(clean)
        if (waMatch != null) {
            val contact = if (waMatch.groupValues.size > 1 && waMatch.groupValues[1].isNotBlank()) waMatch.groupValues[1].trim() else "Contact"
            val messageText = if (waMatch.groupValues.size > 2 && waMatch.groupValues[2].isNotBlank()) waMatch.groupValues[2].trim() else clean
            return@withContext ParsedIntent.ConsequentialConfirmation(
                warning = "WhatsApp पर $contact को यह मैसेज भेजना चाहते हैं: \"$messageText\"? भेजने से पहले पुष्टि करें।",
                pendingCommand = ActionCommand.SendMessage("WhatsApp", contact, messageText)
            )
        }

        // 7. General App Launching
        val appKeywords = listOf(
            "youtube", "whatsapp", "chrome", "camera", "calculator",
            "settings", "maps", "map", "gmail", "mail", "clock", "alarm", "phone", "dialer"
        )
        for (app in appKeywords) {
            if (lower.contains("$app kholo") || lower.contains("open $app") ||
                lower.contains("$app chalu karo") || lower.contains("$app open karo") ||
                lower == app) {
                val appTitle = app.replaceFirstChar { it.uppercase() }
                return@withContext ParsedIntent.DirectAction(
                    command = ActionCommand.LaunchApp(app, "", appTitle),
                    responseSpeech = "हाँ जी! $app खोल रही हूँ। (Opening $appTitle)"
                )
            }
        }

        // 8. Polite greetings & identity in Hindi/Hinglish/English
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") || lower.contains("kaise ho") || lower.contains("who are you") || lower.contains("aap kaun ho")) {
            val greetings = listOf(
                "नमस्ते! मैं $assistantName हूँ, आपकी पर्सनल एनिमे वॉइस असिस्टेंट! कहिए, आज मैं आपकी क्या मदद करूँ? 💕",
                "Hello! I am $assistantName, ready to assist! Aap YouTube open karne, WhatsApp message likhne, ya screen samjhane ke liye keh sakte hain! ✨",
                "Konichiwa! Heena AI yahan hai. Aapki service mein hamesha taiyyar! Bataiye kya kaam hai? 🌸"
            )
            return@withContext ParsedIntent.Conversational(greetings.random())
        }

        // 9. AI reasoning with Gemini API if key is available
        val configuredKey = apiKeyManager?.getApiKey()
        val targetModel = apiKeyManager?.model?.value ?: "gemini-1.5-flash"
        val targetEndpoint = apiKeyManager?.customEndpoint?.value ?: "https://generativelanguage.googleapis.com"

        val apiKey = if (!configuredKey.isNullOrBlank()) {
            configuredKey
        } else {
            try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                field.get(null) as? String ?: ""
            } catch (e: Throwable) {
                ""
            }
        }

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val geminiReply = queryGeminiModel(clean, assistantName, apiKey, targetModel, targetEndpoint)
                if (geminiReply.isNotBlank()) {
                    return@withContext ParsedIntent.Conversational(geminiReply)
                }
            } catch (e: Exception) {
                Log.w("ConversationEngine", "Gemini API query fallback", e)
            }
        }

        // 10. Intelligent offline Hinglish conversational responses
        val fallbackReply = generateSmartOfflineReply(clean, assistantName)
        ParsedIntent.Conversational(fallbackReply)
    }

    private fun queryGeminiModel(
        userPrompt: String,
        assistantName: String,
        apiKey: String,
        model: String = "gemini-1.5-flash",
        baseUrl: String = "https://generativelanguage.googleapis.com"
    ): String {
        val systemPrompt = "You are $assistantName, a cute, extremely polite, cheerful and intelligent anime girl voice assistant for Android. Respond naturally in friendly Hinglish (blend of Hindi and English) or English as appropriate. Keep answers concise, helpful, and sweet (2-3 sentences max) because your response will be read aloud via Text-to-Speech."
        val cleanBase = baseUrl.removeSuffix("/")
        val url = "$cleanBase/v1beta/models/$model:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\nUser: $userPrompt\n$assistantName:"))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return ""
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "").trim()
                    }
                }
            }
        }
        return ""
    }

    private fun generateSmartOfflineReply(query: String, assistantName: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("time") || lower.contains("samay") || lower.contains("waqt") -> {
                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                "Abhi samay hai: ${sdf.format(java.util.Date())} ⏰"
            }
            lower.contains("date") || lower.contains("taarikh") || lower.contains("aaj kaun sa din") -> {
                val sdf = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.getDefault())
                "Aaj ki taarikh hai: ${sdf.format(java.util.Date())} 📅"
            }
            lower.contains("joke") || lower.contains("chutkula") || lower.contains("hansaao") -> {
                "Hehe, ek majedaar baat suniye: Ek AI ne doosre AI se pucha, 'Tum itna khush kyun ho?' Usne kaha, 'Kyunki meri battery 100% hai!' 😄💖"
            }
            lower.contains("gaana") || lower.contains("song") || lower.contains("sing") -> {
                "Main aapke liye YouTube par gaana chala sakti hoon! Bas kahiye: 'YouTube par Arijit Singh ke gaane chalao' 🎶"
            }
            lower.contains("thank") || lower.contains("shukriya") || lower.contains("dhanyawad") -> {
                "You're very welcome! Aapki madad karke mujhe bahut khushi mili! 🌸✨"
            }
            else -> {
                "Maine aapki baat suni: '$query'. Main ise Google par search kar sakti hoon ya koi app khol sakti hoon. Kahiye toh search karoon? 💕"
            }
        }
    }
}
