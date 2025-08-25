package dev.albertus.expensms.data.repository

import android.util.Log
import dev.albertus.expensms.data.local.SenderFilterDao
import dev.albertus.expensms.data.model.DefaultSenderFilters
import dev.albertus.expensms.data.model.SenderFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SenderFilterRepository @Inject constructor(
    private val senderFilterDao: SenderFilterDao
) {
    companion object {
        private const val TAG = "SenderFilterRepository"
    }

    fun getAllSenderFiltersFlow(): Flow<List<SenderFilter>> {
        return senderFilterDao.getAllSenderFiltersFlow()
    }

    fun getEnabledSenderFiltersFlow(): Flow<List<SenderFilter>> {
        return senderFilterDao.getEnabledSenderFiltersFlow()
    }

    suspend fun getAllSenderFilters(): List<SenderFilter> {
        return withContext(Dispatchers.IO) {
            senderFilterDao.getAllSenderFilters()
        }
    }

    suspend fun getEnabledSenderFilters(): List<SenderFilter> {
        return withContext(Dispatchers.IO) {
            senderFilterDao.getEnabledSenderFilters()
        }
    }

    suspend fun getSenderFilterById(id: String): SenderFilter? {
        return withContext(Dispatchers.IO) {
            senderFilterDao.getSenderFilterById(id)
        }
    }

    suspend fun insertSenderFilter(senderFilter: SenderFilter) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Inserting sender filter: ${senderFilter.name}")
            senderFilterDao.insertSenderFilter(senderFilter)
        }
    }

    suspend fun insertSenderFilters(senderFilters: List<SenderFilter>) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Inserting ${senderFilters.size} sender filters")
            senderFilterDao.insertSenderFilters(senderFilters)
        }
    }

    suspend fun updateSenderFilter(senderFilter: SenderFilter) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Updating sender filter: ${senderFilter.name}")
            val updatedFilter = senderFilter.copy(updatedAt = Date())
            senderFilterDao.updateSenderFilter(updatedFilter)
        }
    }

    suspend fun updateSenderFilterEnabled(id: String, isEnabled: Boolean) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Updating sender filter $id enabled status to: $isEnabled")
            senderFilterDao.updateSenderFilterEnabled(id, isEnabled, Date())
        }
    }

    suspend fun deleteSenderFilter(senderFilter: SenderFilter) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Deleting sender filter: ${senderFilter.name}")
            senderFilterDao.deleteSenderFilter(senderFilter)
        }
    }

    suspend fun deleteSenderFilterById(id: String) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Deleting sender filter by ID: $id")
            senderFilterDao.deleteSenderFilterById(id)
        }
    }

    suspend fun initializeDefaultFilters() {
        withContext(Dispatchers.IO) {
            val existingCount = senderFilterDao.getSenderFilterCount()
            if (existingCount == 0) {
                Log.i(TAG, "No sender filters found, initializing with defaults")
                senderFilterDao.insertSenderFilters(DefaultSenderFilters.defaults)
                Log.i(TAG, "Initialized ${DefaultSenderFilters.defaults.size} default sender filters")
            } else {
                Log.d(TAG, "Found $existingCount existing sender filters, skipping initialization")
            }
        }
    }

    /**
     * Check if a sender matches any of the enabled filters
     */
    suspend fun matchesSender(sender: String): SenderFilter? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "=== MATCHING SENDER: '$sender' ===")
            val enabledFilters = getEnabledSenderFilters()
            Log.d(TAG, "Enabled filters count: ${enabledFilters.size}")

            if (enabledFilters.isEmpty()) {
                Log.w(TAG, "❌ No enabled filters found!")
                return@withContext null
            }

            enabledFilters.forEach { filter ->
                val matches = sender.contains(filter.filterPattern, ignoreCase = true)
                Log.d(TAG, "Testing filter '${filter.name}' (pattern: '${filter.filterPattern}') -> ${if (matches) "✅ MATCH" else "❌ NO MATCH"}")
                if (matches) {
                    Log.i(TAG, "✅ FOUND MATCH: Filter '${filter.name}' matches sender '$sender'")
                    return@withContext filter
                }
            }

            Log.w(TAG, "❌ NO MATCH: Sender '$sender' does not match any enabled filters")
            return@withContext null
        }
    }

    /**
     * Get the bank name for a sender if it matches any filter
     */
    suspend fun getBankNameForSender(sender: String): String? {
        return matchesSender(sender)?.name
    }
}
