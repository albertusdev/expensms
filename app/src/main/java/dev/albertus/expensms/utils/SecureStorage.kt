package dev.albertus.expensms.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = createEncryptedPreferences()

    private fun createEncryptedPreferences(): SharedPreferences {
        return try {
            Log.d(TAG, "Attempting to create encrypted preferences")
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create encrypted preferences, attempting recovery", e)
            handleEncryptionFailure(e)
        }
    }

    private fun handleEncryptionFailure(originalException: Exception): SharedPreferences {
        return try {
            // First, try to delete the corrupted preferences file
            Log.i(TAG, "Attempting to clear corrupted encrypted preferences")
            context.deleteSharedPreferences(PREFS_NAME)

            // Try to create encrypted preferences again
            Log.i(TAG, "Attempting to recreate encrypted preferences after cleanup")
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recover encrypted preferences, falling back to regular SharedPreferences", e)
            // As a last resort, fall back to regular SharedPreferences
            // This is not ideal for security but prevents app crashes
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val TAG = "SecureStorage"
        private const val PREFS_NAME = "expensms_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "expensms_secure_prefs_fallback"
        private const val KEY_API_PASSWORD = "api_password"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }

    fun saveApiPassword(password: String) {
        safeEdit {
            putString(KEY_API_PASSWORD, password)
        }
    }

    fun getApiPassword(): String? {
        return safeGet { getString(KEY_API_PASSWORD, null) }
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiryTimeMillis: Long = 0L) {
        safeEdit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis)
        }
    }

    fun getAccessToken(): String? {
        return safeGet { getString(KEY_ACCESS_TOKEN, null) }
    }

    fun getRefreshToken(): String? {
        return safeGet { getString(KEY_REFRESH_TOKEN, null) }
    }

    fun getTokenExpiry(): Long {
        return safeGet { getLong(KEY_TOKEN_EXPIRY, 0L) } ?: 0L
    }

    fun isTokenExpired(): Boolean {
        val expiry = getTokenExpiry()
        return expiry > 0 && System.currentTimeMillis() >= expiry
    }

    fun clearTokens() {
        safeEdit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRY)
        }
    }

    fun clearAll() {
        safeEdit { clear() }
    }

    /**
     * Safely performs a read operation on SharedPreferences with error handling
     */
    private fun <T> safeGet(operation: SharedPreferences.() -> T): T? {
        return try {
            encryptedPrefs.operation()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read from encrypted preferences", e)
            null
        }
    }

    /**
     * Safely performs a write operation on SharedPreferences with error handling
     */
    private fun safeEdit(operation: SharedPreferences.Editor.() -> Unit) {
        try {
            encryptedPrefs.edit().apply {
                operation()
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to encrypted preferences", e)
        }
    }
}
