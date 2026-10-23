package com.example.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ApiProvider(val displayName: String, val defaultEndpoint: String) {
    GEMINI("Google Gemini API", "https://generativelanguage.googleapis.com"),
    CUSTOM_ENDPOINT("AI Studio / Compatible Endpoint", "https://generativelanguage.googleapis.com")
}

data class ConnectionTestResult(
    val success: Boolean,
    val statusCode: Int,
    val latencyMs: Long,
    val message: String,
    val sampleResponse: String? = null
)

class ApiKeyManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("heena_api_vault", Context.MODE_PRIVATE)

    private val _provider = MutableStateFlow(loadProvider())
    val provider: StateFlow<ApiProvider> = _provider.asStateFlow()

    private val _model = MutableStateFlow(prefs.getString(KEY_MODEL, "gemini-1.5-flash") ?: "gemini-1.5-flash")
    val model: StateFlow<String> = _model.asStateFlow()

    private val _customEndpoint = MutableStateFlow(prefs.getString(KEY_ENDPOINT, "https://generativelanguage.googleapis.com") ?: "https://generativelanguage.googleapis.com")
    val customEndpoint: StateFlow<String> = _customEndpoint.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionTestResult?>(null)
    val connectionStatus: StateFlow<ConnectionTestResult?> = _connectionStatus.asStateFlow()

    private val _hasKey = MutableStateFlow(hasStoredKey())
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val KEY_ENCRYPTED_API_KEY = "enc_api_key"
        private const val KEY_PROVIDER = "api_provider"
        private const val KEY_MODEL = "api_model"
        private const val KEY_ENDPOINT = "api_endpoint"

        val AVAILABLE_MODELS = listOf(
            "gemini-1.5-flash" to "Gemini 1.5 Flash (Fast, Recommended)",
            "gemini-1.5-pro" to "Gemini 1.5 Pro (Deep Reasoning)",
            "gemini-2.0-flash" to "Gemini 2.0 Flash (Next-Gen Preview)",
            "custom-model" to "Custom Model"
        )
    }

    private fun loadProvider(): ApiProvider {
        val stored = prefs.getString(KEY_PROVIDER, ApiProvider.GEMINI.name) ?: ApiProvider.GEMINI.name
        return try {
            ApiProvider.valueOf(stored)
        } catch (e: Exception) {
            ApiProvider.GEMINI
        }
    }

    fun hasStoredKey(): Boolean {
        val raw = prefs.getString(KEY_ENCRYPTED_API_KEY, null)
        return !raw.isNullOrBlank()
    }

    fun getApiKey(): String {
        val enc = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return ""
        return decryptKey(enc)
    }

    fun getMaskedApiKey(): String {
        val key = getApiKey()
        if (key.length <= 8) return if (key.isNotEmpty()) "••••••••" else "Not Configured"
        val prefix = key.take(4)
        val suffix = key.takeLast(4)
        return "$prefix••••••••$suffix"
    }

    fun saveConfiguration(apiKey: String, provider: ApiProvider, model: String, endpoint: String) {
        val enc = encryptKey(apiKey.trim())
        prefs.edit()
            .putString(KEY_ENCRYPTED_API_KEY, enc)
            .putString(KEY_PROVIDER, provider.name)
            .putString(KEY_MODEL, model.trim())
            .putString(KEY_ENDPOINT, endpoint.trim())
            .apply()

        _provider.value = provider
        _model.value = model.trim()
        _customEndpoint.value = endpoint.trim()
        _hasKey.value = apiKey.trim().isNotEmpty()
        _connectionStatus.value = null
    }

    fun removeApiKey() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_API_KEY)
            .apply()
        _hasKey.value = false
        _connectionStatus.value = null
    }

    suspend fun testConnection(): ConnectionTestResult = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            val result = ConnectionTestResult(
                success = false,
                statusCode = 0,
                latencyMs = 0,
                message = "API Key दर्ज नहीं की गई है। (No API key configured)"
            )
            _connectionStatus.value = result
            return@withContext result
        }

        val startTime = System.currentTimeMillis()
        val currentModel = _model.value
        val baseUrl = _customEndpoint.value.removeSuffix("/")
        val url = "$baseUrl/v1beta/models/$currentModel:generateContent?key=$key"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Hello Heena. Respond with one word: Online"))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val code = response.code
                val bodyStr = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val candidateText = try {
                        val json = JSONObject(bodyStr)
                        json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim()
                    } catch (e: Exception) {
                        "Connection established"
                    }

                    val result = ConnectionTestResult(
                        success = true,
                        statusCode = code,
                        latencyMs = latency,
                        message = "सफलतापूर्वक कनेक्ट हो गया! (Connected successfully in ${latency}ms)",
                        sampleResponse = candidateText
                    )
                    _connectionStatus.value = result
                    return@withContext result
                } else {
                    val errorDetail = when (code) {
                        400 -> "अमान्य अनुरोध या मॉडल अनुपलब्ध (Invalid request / Model not supported: $currentModel)"
                        401, 403 -> "API Key अमान्य है या अनुमतियाँ नहीं हैं (Invalid API Key or forbidden)"
                        429 -> "API कोटा समाप्त हो गया (Quota exceeded / Rate limited)"
                        else -> "HTTP $code: सर्वर त्रुटि (Server error)"
                    }
                    val result = ConnectionTestResult(
                        success = false,
                        statusCode = code,
                        latencyMs = latency,
                        message = errorDetail
                    )
                    _connectionStatus.value = result
                    return@withContext result
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val result = ConnectionTestResult(
                success = false,
                statusCode = -1,
                latencyMs = latency,
                message = "नेटवर्क कनेक्शन त्रुटि: ${e.localizedMessage ?: "Network failed"}"
            )
            _connectionStatus.value = result
            return@withContext result
        }
    }

    // Obfuscation/encryption layer to protect key from plain text inspection
    private fun encryptKey(plainKey: String): String {
        if (plainKey.isEmpty()) return ""
        val xorKey = context.packageName.hashCode().toByte()
        val bytes = plainKey.toByteArray(Charsets.UTF_8)
        val encrypted = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor xorKey.toInt()).toByte() }
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decryptKey(encoded: String): String {
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val xorKey = context.packageName.hashCode().toByte()
            val decrypted = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor xorKey.toInt()).toByte() }
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
