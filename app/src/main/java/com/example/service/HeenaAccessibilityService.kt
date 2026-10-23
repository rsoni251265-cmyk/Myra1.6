package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenElement(
    val text: String,
    val className: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val bounds: Rect
)

data class ScreenAnalysis(
    val appPackage: String?,
    val visibleTexts: List<String>,
    val clickableElements: List<ScreenElement>,
    val editableFields: List<ScreenElement>,
    val summaryHinglish: String
)

class HeenaAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: HeenaAccessibilityService? = null

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastScreenAnalysis = MutableStateFlow<ScreenAnalysis?>(null)
        val lastScreenAnalysis: StateFlow<ScreenAnalysis?> = _lastScreenAnalysis.asStateFlow()

        private val _isAutomationPaused = MutableStateFlow(false)
        val isAutomationPaused: StateFlow<Boolean> = _isAutomationPaused.asStateFlow()

        fun isConnected(): Boolean = instance != null

        fun analyzeCurrentScreen(): ScreenAnalysis? {
            return instance?.performScreenAnalysis()
        }

        fun scrollScreen(forward: Boolean): Boolean {
            if (_isAutomationPaused.value) return false
            return instance?.performScroll(forward) ?: false
        }

        fun clickElementWithText(targetText: String): Boolean {
            if (_isAutomationPaused.value) return false
            return instance?.performClickOnText(targetText) ?: false
        }

        fun pauseAutomation(paused: Boolean) {
            _isAutomationPaused.value = paused
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.d("HeenaAccessibility", "Heena Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Automatically keep latest screen summary fresh when window changes
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            performScreenAnalysis()
        }
    }

    override fun onInterrupt() {
        Log.w("HeenaAccessibility", "Heena Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    fun performScreenAnalysis(): ScreenAnalysis? {
        val rootNode = rootInActiveWindow ?: return null
        val texts = mutableListOf<String>()
        val clickables = mutableListOf<ScreenElement>()
        val editables = mutableListOf<ScreenElement>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            // Privacy protection: Skip password nodes
            if (node.isPassword) return

            val text = node.text?.toString()?.trim()
            val contentDesc = node.contentDescription?.toString()?.trim()
            val effectiveLabel = when {
                !text.isNullOrBlank() -> text
                !contentDesc.isNullOrBlank() -> contentDesc
                else -> null
            }

            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            if (!effectiveLabel.isNullOrBlank() && bounds.width() > 0 && bounds.height() > 0) {
                texts.add(effectiveLabel)
                val element = ScreenElement(
                    text = effectiveLabel,
                    className = node.className?.toString() ?: "",
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    bounds = bounds
                )
                if (node.isClickable) clickables.add(element)
                if (node.isEditable) editables.add(element)
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        try {
            traverse(rootNode)
            val pkg = rootNode.packageName?.toString()

            val previewItems = texts.take(6).joinToString(", ")
            val buttonCount = clickables.size
            val summary = if (texts.isNotEmpty()) {
                "स्क्रीन पर $buttonCount क्लिक करने योग्य बटन और मुख्य जानकारी दिख रही है: $previewItems"
            } else {
                "स्क्रीन पर कोई मुख्य टेक्स्ट नहीं मिला।"
            }

            val analysis = ScreenAnalysis(
                appPackage = pkg,
                visibleTexts = texts.distinct(),
                clickableElements = clickables,
                editableFields = editables,
                summaryHinglish = summary
            )
            _lastScreenAnalysis.value = analysis
            return analysis
        } catch (e: Exception) {
            Log.e("HeenaAccessibility", "Error during screen analysis", e)
            return null
        }
    }

    fun performScroll(forward: Boolean): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }

        fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                val found = findScrollable(node.getChild(i))
                if (found != null) return found
            }
            return null
        }

        val scrollNode = findScrollable(rootNode)
        return scrollNode?.performAction(action) ?: false
    }

    fun performClickOnText(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        for (node in nodes) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            // Check parent if parent is clickable
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
            }
        }
        return false
    }
}
