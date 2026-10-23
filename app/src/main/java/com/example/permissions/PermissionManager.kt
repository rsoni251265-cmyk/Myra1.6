package com.example.permissions

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.view.accessibility.AccessibilityManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.service.HeenaAccessibilityService
import com.example.service.HeenaAssistantVoiceService

enum class PermissionState(val displayName: String) {
    ALLOWED("अनुमत (Allowed)"),
    DENIED("अस्वीकृत (Denied)"),
    NEEDS_SETUP("सेटअप आवश्यक (Needs Setup)")
}

data class PermissionDetail(
    val id: String,
    val name: String,
    val usage: String,
    val currentStatus: PermissionState,
    val limitationIfDenied: String,
    val manifestPermission: String? = null,
    val isSystemSettingsRequired: Boolean = false
)

class PermissionManager(private val context: Context) {

    fun getPermissionsList(): List<PermissionDetail> {
        val list = mutableListOf<PermissionDetail>()

        // 1. Microphone
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionDetail(
                id = "mic",
                name = "Microphone Access (माइक)",
                usage = "Used to detect the 'Hello Heena' wake word and listen for voice instructions in Hindi, English, and Hinglish.",
                currentStatus = if (micGranted) PermissionState.ALLOWED else PermissionState.DENIED,
                limitationIfDenied = "Voice commands and wake word will not function; you will have to type commands manually.",
                manifestPermission = Manifest.permission.RECORD_AUDIO
            )
        )

        // 2. Camera Access
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionDetail(
                id = "camera",
                name = "Camera Access (कैमरा)",
                usage = "Used for face/expression recognition personalization or visual context queries when authorized.",
                currentStatus = if (cameraGranted) PermissionState.ALLOWED else PermissionState.DENIED,
                limitationIfDenied = "Camera-based personalization and visual features remain disabled.",
                manifestPermission = Manifest.permission.CAMERA
            )
        )

        // 3. Notifications
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        list.add(
            PermissionDetail(
                id = "notifications",
                name = "Notifications (सूचनाएं)",
                usage = "Displays persistent foreground status notification showing background listening status and emergency stop action.",
                currentStatus = if (notifGranted) PermissionState.ALLOWED else PermissionState.DENIED,
                limitationIfDenied = "You will not see the active listening banner in the Android notification drawer.",
                manifestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
            )
        )

        // 4. Accessibility Service
        val accessibilityActive = HeenaAccessibilityService.isServiceActive.value || isAccessibilityServiceEnabled()
        list.add(
            PermissionDetail(
                id = "accessibility",
                name = "Accessibility Service (स्क्रीन असिस्टेंट)",
                usage = "Allows Heena to read on-screen text when asked 'Heena screen samjhao' and scroll windows on voice command.",
                currentStatus = if (accessibilityActive) PermissionState.ALLOWED else PermissionState.NEEDS_SETUP,
                limitationIfDenied = "Screen understanding ('screen par kya dikh raha hai') and voice-driven scrolling will not work.",
                isSystemSettingsRequired = true
            )
        )

        // 5. Background Foreground Service
        val bgServiceRunning = HeenaAssistantVoiceService.isRunning.value
        list.add(
            PermissionDetail(
                id = "foreground_service",
                name = "Background Foreground Service (बैकग्राउंड सेवा)",
                usage = "Keeps wake-word listening active while using other apps or on the home screen.",
                currentStatus = if (bgServiceRunning) PermissionState.ALLOWED else PermissionState.NEEDS_SETUP,
                limitationIfDenied = "Heena AI only responds while the application is actively open on screen.",
                isSystemSettingsRequired = false
            )
        )

        // 6. Speech Recognition Engine
        val speechAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        list.add(
            PermissionDetail(
                id = "speech_engine",
                name = "Speech Recognition Engine (आवाज़ पहचान)",
                usage = "Device-level or Google Speech Recognizer service that converts audio voice to text.",
                currentStatus = if (speechAvailable) PermissionState.ALLOWED else PermissionState.DENIED,
                limitationIfDenied = "Speech-to-text conversion is unavailable on device; install Google Speech Services.",
                isSystemSettingsRequired = false
            )
        )

        // 7. Photo / Media Access
        val photoPickerSupported = ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)
        list.add(
            PermissionDetail(
                id = "photo_picker",
                name = "Photo & Media Selection (फ़ोटो एक्सेस)",
                usage = "Privacy-friendly Android Photo Picker allows selecting custom anime avatars without granting full storage permission.",
                currentStatus = if (photoPickerSupported) PermissionState.ALLOWED else PermissionState.NEEDS_SETUP,
                limitationIfDenied = "Default bundled anime avatars will be used instead of custom gallery photos."
            )
        )

        return list
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    }

    fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
