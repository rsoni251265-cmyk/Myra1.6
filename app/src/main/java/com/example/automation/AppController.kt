package com.example.automation

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import java.net.URLEncoder

data class ActionResult(
    val success: Boolean,
    val feedbackMessage: String,
    val targetApp: String? = null
)

class AppController(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Launch an application by name or package
     */
    fun launchAppByName(rawName: String): ActionResult {
        val clean = rawName.lowercase().trim()
        val (pkg, displayName) = when {
            clean.contains("youtube") -> "com.google.android.youtube" to "YouTube"
            clean.contains("whatsapp") -> "com.whatsapp" to "WhatsApp"
            clean.contains("chrome") || clean.contains("browser") -> "com.android.chrome" to "Google Chrome"
            clean.contains("map") -> "com.google.android.apps.maps" to "Google Maps"
            clean.contains("gmail") || clean.contains("email") || clean.contains("mail") -> "com.google.android.gm" to "Gmail"
            clean.contains("camera") -> return openCamera()
            clean.contains("calculator") -> return openCalculator()
            clean.contains("setting") -> return openSettings()
            clean.contains("dialer") || clean.contains("phone") || clean.contains("call") -> return openDialer()
            clean.contains("clock") || clean.contains("alarm") -> return openClock()
            else -> null to null
        }

        if (pkg != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult(
                    success = true,
                    feedbackMessage = "ठीक है, $displayName खोल रही हूँ! (Opening $displayName)",
                    targetApp = displayName
                )
            } else {
                // Try fallback intent or store link
                return ActionResult(
                    success = false,
                    feedbackMessage = "$displayName आपके फोन में इंस्टॉल नहीं है। (App $displayName is not installed)",
                    targetApp = displayName
                )
            }
        }

        // Generic query for installed app matching name
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(intent, 0)
        for (info in apps) {
            val appLabel = info.loadLabel(packageManager).toString()
            if (appLabel.lowercase().contains(clean)) {
                val foundIntent = packageManager.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (foundIntent != null) {
                    foundIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(foundIntent)
                    return ActionResult(
                        success = true,
                        feedbackMessage = "जी! $appLabel खोल दिया है। (Opening $appLabel)",
                        targetApp = appLabel
                    )
                }
            }
        }

        return ActionResult(
            success = false,
            feedbackMessage = "माफ़ कीजिये, मुझे '$rawName' ऐप नहीं मिला। (Could not find application '$rawName')"
        )
    }

    fun searchYouTube(query: String): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                ActionResult(true, "YouTube पर '$query' खोज रही हूँ! (Searching YouTube for '$query')", "YouTube")
            } else {
                // Fallback to web browser YouTube
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                ActionResult(true, "Browser में YouTube पर '$query' खोज रही हूँ!", "YouTube")
            }
        } catch (e: Exception) {
            Log.e("AppController", "Failed to search YouTube", e)
            ActionResult(false, "YouTube खोलने में समस्या आई।")
        }
    }

    fun searchWeb(query: String): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult(true, "Google पर '$query' सर्च कर रही हूँ! (Searching Google for '$query')", "Google Search")
        } catch (e: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ActionResult(true, "Google सर्च खोल दिया है।", "Google")
        }
    }

    fun draftWhatsAppMessage(recipient: String, message: String): ActionResult {
        return try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                ActionResult(true, "WhatsApp पर $recipient के लिए मैसेज तैयार कर दिया है: '$message'", "WhatsApp")
            } else {
                // Fallback to generic SMS share
                val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:")
                    putExtra("sms_body", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
                ActionResult(true, "मैसेज तैयार कर दिया है: '$message'", "Messages")
            }
        } catch (e: Exception) {
            Log.e("AppController", "Error drafting message", e)
            ActionResult(false, "मैसेज ड्राफ्ट करने में असमर्थ।")
        }
    }

    fun draftEmail(recipient: String = "", subject: String = "", body: String = ""): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${if (recipient.isNotBlank()) recipient else ""}")
                if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
                if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult(true, "ईमेल कंपोज़र खोल दिया है। (Opened Email composer)", "Gmail")
        } catch (e: Exception) {
            ActionResult(false, "ईमेल ऐप खोलने में असमर्थ।")
        }
    }

    private fun openCamera(): ActionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult(true, "कैमरा खोल रही हूँ! (Opening Camera)", "Camera")
        } catch (e: Exception) {
            ActionResult(false, "कैमरा खोलने में असमर्थ।")
        }
    }

    private fun openDialer(): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult(true, "फ़ोन डायलर खोल रही हूँ! (Opening Phone Dialer)", "Phone")
        } catch (e: Exception) {
            ActionResult(false, "डायलर खोलने में असमर्थ।")
        }
    }

    private fun openSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult(true, "फोन सेटिंग्स खोल दी है। (Opening Device Settings)", "Settings")
        } catch (e: Exception) {
            ActionResult(false, "सेटिंग्स खोलने में असमर्थ।")
        }
    }

    private fun openCalculator(): ActionResult {
        val calcPackages = listOf(
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.sec.android.app.popupcalculator"
        )
        for (pkg in calcPackages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return ActionResult(true, "कैलकुलेटर खोल दिया है। (Opening Calculator)", "Calculator")
            }
        }
        return ActionResult(false, "कैलकुलेटर ऐप नहीं मिला।")
    }

    private fun openClock(): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult(true, "घड़ी और अलार्म खोल दिया है। (Opening Clock)", "Clock")
        } catch (e: Exception) {
            ActionResult(false, "क्लॉक ऐप खोलने में असमर्थ।")
        }
    }
}
