package dev.albertus.expensms.ui.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.albertus.expensms.data.api.ApiService
import dev.albertus.expensms.data.repository.UserPreferencesRepository
import dev.albertus.expensms.utils.SecureStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val apiService: ApiService,
    private val secureStorage: SecureStorage
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    val apiForwardingEnabled: StateFlow<Boolean> = userPreferencesRepository.apiForwardingEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val apiEmail: StateFlow<String> = userPreferencesRepository.apiEmail
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val _apiTestResult = MutableStateFlow<String?>(null)
    val apiTestResult: StateFlow<String?> = _apiTestResult.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    private val _apiPassword = MutableStateFlow("")
    val apiPassword: StateFlow<String> = _apiPassword.asStateFlow()

    init {
        // Load stored password on initialization
        viewModelScope.launch {
            val storedPassword = secureStorage.getApiPassword()
            _apiPassword.value = storedPassword ?: ""
        }
    }

    fun setApiForwardingEnabled(enabled: Boolean) {
        Log.i(TAG, "Setting API forwarding enabled: $enabled")
        viewModelScope.launch {
            userPreferencesRepository.setApiForwardingEnabled(enabled)
        }
    }

    fun setApiEmail(email: String) {
        Log.i(TAG, "Setting API email: $email")
        viewModelScope.launch {
            userPreferencesRepository.setApiEmail(email)
        }
    }

    fun setApiPassword(password: String) {
        Log.i(TAG, "Setting API password")
        _apiPassword.value = password
        viewModelScope.launch {
            secureStorage.saveApiPassword(password)
        }
    }

    fun testApiConnection() {
        Log.i(TAG, "=== TESTING API CONNECTION ===")
        if (_isTestingApi.value) {
            Log.w(TAG, "API test already in progress")
            return
        }

        viewModelScope.launch {
            try {
                _isTestingApi.value = true
                _apiTestResult.value = null
                Log.i(TAG, "Starting API connection test...")

                // For now, just check if API is configured
                val isConfigured = apiService.isApiConfigured()
                Log.i(TAG, "API configured: $isConfigured")

                _apiTestResult.value = if (isConfigured) {
                    "✅ API credentials are configured!"
                } else {
                    "❌ API credentials not configured. Please set email and password."
                }
            } catch (e: Exception) {
                Log.e(TAG, "API test failed with exception", e)
                _apiTestResult.value = "❌ API test failed: ${e.message}"
            } finally {
                _isTestingApi.value = false
            }
        }
    }

    fun clearApiTestResult() {
        Log.d(TAG, "Clearing API test result")
        _apiTestResult.value = null
    }

    fun clearApiConfiguration() {
        Log.i(TAG, "=== CLEARING API CONFIGURATION ===")
        viewModelScope.launch {
            try {
                userPreferencesRepository.setApiForwardingEnabled(false)
                userPreferencesRepository.setApiEmail("")
                secureStorage.clearAll()
                _apiPassword.value = ""
                _apiTestResult.value = null
                Log.i(TAG, "✅ API configuration cleared")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear API configuration", e)
            }
        }
    }
}
