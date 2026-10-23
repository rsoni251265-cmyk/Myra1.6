package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ConversationEngine
import com.example.ai.ParsedIntent
import com.example.api.ApiKeyManager
import com.example.api.ApiProvider
import com.example.data.PreferencesManager
import com.example.integrations.AppIntegrationManager
import com.example.model.ActionCommand
import com.example.permissions.PermissionManager
import com.example.security.SecurityManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Heena AI", appName)
    }

    @Test
    fun testPreferencesManagerDefaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        assertEquals("Heena AI", prefs.settings.value.assistantName)
        assertEquals("Hello Heena", prefs.settings.value.wakePhrase)
        assertEquals("Hinglish", prefs.settings.value.language)
    }

    @Test
    fun testConversationEngineIntentParsing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = ConversationEngine(context)

        // Test App Launch
        val ytIntent = engine.processQuery("YouTube kholo", "Heena AI")
        assertTrue(ytIntent is ParsedIntent.DirectAction)
        val cmd = (ytIntent as ParsedIntent.DirectAction).command
        assertTrue(cmd is ActionCommand.LaunchApp)

        // Test Screen Understanding
        val screenIntent = engine.processQuery("Heena, screen samjhao", "Heena AI")
        assertTrue(screenIntent is ParsedIntent.DirectAction)
        assertTrue((screenIntent as ParsedIntent.DirectAction).command is ActionCommand.ScreenUnderstanding)

        // Test Consequential Confirmation for sensitive actions
        val resetIntent = engine.processQuery("sab data delete karo", "Heena AI")
        assertTrue(resetIntent is ParsedIntent.ConsequentialConfirmation)
    }

    @Test
    fun testApiKeyManagerStorageAndMasking() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apiKeyManager = ApiKeyManager(context)

        val testKey = "AIzaSyDUMMYTESTKEY12345"
        apiKeyManager.saveConfiguration(testKey, ApiProvider.GEMINI, "gemini-1.5-flash", "https://generativelanguage.googleapis.com")

        assertTrue(apiKeyManager.hasKey.value)
        assertEquals(testKey, apiKeyManager.getApiKey())

        val masked = apiKeyManager.getMaskedApiKey()
        assertTrue(masked.startsWith("AIza"))
        assertTrue(masked.endsWith("2345"))
        assertTrue(masked.contains("••••"))

        apiKeyManager.removeApiKey()
        assertFalse(apiKeyManager.hasKey.value)
        assertEquals("", apiKeyManager.getApiKey())
    }

    @Test
    fun testSecurityManagerPinHashingAndVerification() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)

        // Setting a 4-digit PIN
        assertTrue(securityManager.setPin("1234"))
        assertTrue(securityManager.hasPin())
        assertTrue(securityManager.isAppLockEnabled.value)

        // Verification
        assertTrue(securityManager.verifyPin("1234"))
        assertFalse(securityManager.verifyPin("9999"))
        assertFalse(securityManager.verifyPin("0000"))

        // Auto lock timeout test
        securityManager.setAutoLockTimeout(15)
        assertEquals(15, securityManager.autoLockTimeoutMinutes.value)

        // Reset
        securityManager.removeAppLock()
        assertFalse(securityManager.hasPin())
        assertFalse(securityManager.isAppLockEnabled.value)
    }

    @Test
    fun testPermissionManagerList() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val permissionManager = PermissionManager(context)

        val list = permissionManager.getPermissionsList()
        assertTrue(list.isNotEmpty())
        assertTrue(list.any { it.id == "mic" })
        assertTrue(list.any { it.id == "camera" })
        assertTrue(list.any { it.id == "notifications" })
        assertTrue(list.any { it.id == "accessibility" })
        assertTrue(list.any { it.id == "foreground_service" })
    }

    @Test
    fun testAppIntegrationManagerToggles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val integrationManager = AppIntegrationManager(context)

        val initial = integrationManager.integrations.value
        assertTrue(initial.any { it.id == "youtube" })
        assertTrue(initial.any { it.id == "whatsapp" })

        // Toggle integration
        integrationManager.toggleIntegration("youtube", false)
        val updated = integrationManager.integrations.value
        assertFalse(updated.first { it.id == "youtube" }.isEnabled)

        // Google connection test
        integrationManager.connectGoogleAccount("testuser@gmail.com")
        assertTrue(integrationManager.isGoogleConnected.value)
        assertEquals("testuser@gmail.com", integrationManager.connectedEmail.value)

        integrationManager.disconnectGoogleAccount()
        assertFalse(integrationManager.isGoogleConnected.value)
    }
}
