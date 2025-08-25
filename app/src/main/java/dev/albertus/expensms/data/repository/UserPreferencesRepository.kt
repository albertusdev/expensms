package dev.albertus.expensms.data.repository

import androidx.datastore.core.DataStore
import dev.albertus.expensms.data.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val userPreferencesDataStore: DataStore<UserPreferences>
) {

    val apiForwardingEnabled: Flow<Boolean> = userPreferencesDataStore.data
        .map { preferences -> preferences.apiForwardingEnabled }

    val apiEmail: Flow<String> = userPreferencesDataStore.data
        .map { preferences -> preferences.apiEmail }

    suspend fun setApiForwardingEnabled(enabled: Boolean) {
        userPreferencesDataStore.updateData { preferences ->
            preferences.toBuilder()
                .setApiForwardingEnabled(enabled)
                .build()
        }
    }

    suspend fun setApiEmail(email: String) {
        userPreferencesDataStore.updateData { preferences ->
            preferences.toBuilder()
                .setApiEmail(email)
                .build()
        }
    }
}
