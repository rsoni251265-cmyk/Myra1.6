package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("heena_security_prefs", Context.MODE_PRIVATE)

    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_LOCK_ENABLED, false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _autoLockTimeoutMinutes = MutableStateFlow(prefs.getInt(KEY_TIMEOUT_MINUTES, 5))
    val autoLockTimeoutMinutes: StateFlow<Int> = _autoLockTimeoutMinutes.asStateFlow()

    private val _protectSensitiveSettings = MutableStateFlow(prefs.getBoolean(KEY_PROTECT_SENSITIVE, true))
    val protectSensitiveSettings: StateFlow<Boolean> = _protectSensitiveSettings.asStateFlow()

    private val _isUnlocked = MutableStateFlow(!_isAppLockEnabled.value)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lastUnlockTimestamp: Long = 0L

    companion object {
        private const val KEY_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_TIMEOUT_MINUTES = "timeout_minutes"
        private const val KEY_PROTECT_SENSITIVE = "protect_sensitive_settings"
    }

    fun hasPin(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltString = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = hashPinWithSalt(pin, saltBytes)

        prefs.edit()
            .putString(KEY_PIN_SALT, saltString)
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()

        _isAppLockEnabled.value = true
        _isUnlocked.value = true
        lastUnlockTimestamp = System.currentTimeMillis()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltString = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val saltBytes = Base64.decode(saltString, Base64.NO_WRAP)
        val computedHash = hashPinWithSalt(pin, saltBytes)

        val matches = constantTimeEquals(storedHash, computedHash)
        if (matches) {
            _isUnlocked.value = true
            lastUnlockTimestamp = System.currentTimeMillis()
        }
        return matches
    }

    fun unlockViaBiometric() {
        _isUnlocked.value = true
        lastUnlockTimestamp = System.currentTimeMillis()
    }

    fun lock() {
        if (_isAppLockEnabled.value) {
            _isUnlocked.value = false
        }
    }

    fun checkShouldLock(): Boolean {
        if (!_isAppLockEnabled.value) return false
        if (autoLockTimeoutMinutes.value == 0) return true // Immediate
        val elapsedMillis = System.currentTimeMillis() - lastUnlockTimestamp
        val timeoutMillis = autoLockTimeoutMinutes.value * 60 * 1000L
        return elapsedMillis > timeoutMillis
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
        _isAppLockEnabled.value = enabled
        if (!enabled) {
            _isUnlocked.value = true
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setAutoLockTimeout(minutes: Int) {
        prefs.edit().putInt(KEY_TIMEOUT_MINUTES, minutes).apply()
        _autoLockTimeoutMinutes.value = minutes
    }

    fun setProtectSensitiveSettings(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECT_SENSITIVE, enabled).apply()
        _protectSensitiveSettings.value = enabled
    }

    fun removeAppLock() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()

        _isAppLockEnabled.value = false
        _isBiometricEnabled.value = false
        _isUnlocked.value = true
    }

    private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
