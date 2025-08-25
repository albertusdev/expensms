package dev.albertus.expensms.ui.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.albertus.expensms.data.model.SenderFilter
import dev.albertus.expensms.data.repository.SenderFilterRepository
import dev.albertus.expensms.utils.NotificationService
import dev.albertus.expensms.utils.SimpleSmsSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SenderFiltersViewModel @Inject constructor(
    private val senderFilterRepository: SenderFilterRepository,
    private val simpleSmsSync: SimpleSmsSync,
    private val notificationService: NotificationService
) : ViewModel() {

    companion object {
        private const val TAG = "SenderFiltersViewModel"
    }

    val senderFilters: StateFlow<List<SenderFilter>> = senderFilterRepository.getAllSenderFiltersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initialize default filters if none exist
        viewModelScope.launch {
            try {
                senderFilterRepository.initializeDefaultFilters()
                Log.d(TAG, "Initialized default sender filters")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize default sender filters", e)
            }
        }
    }

    fun addFilter(name: String, pattern: String) {
        Log.i(TAG, "Adding new sender filter: $name -> $pattern")
        viewModelScope.launch {
            try {
                val filter = SenderFilter(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    filterPattern = pattern,
                    isEnabled = true
                )
                senderFilterRepository.insertSenderFilter(filter)
                Log.i(TAG, "✅ Successfully added sender filter: $name")

                // Re-sync SMS messages to apply new filter and show notification
                triggerSmsResync("Added filter: $name", showNotification = true, filterName = name, filterPattern = pattern)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to add sender filter: $name", e)
            }
        }
    }

    fun toggleFilterEnabled(filterId: String) {
        Log.i(TAG, "Toggling enabled status for filter: $filterId")
        viewModelScope.launch {
            try {
                val filter = senderFilterRepository.getSenderFilterById(filterId)
                if (filter != null) {
                    val newEnabledStatus = !filter.isEnabled
                    senderFilterRepository.updateSenderFilterEnabled(filterId, newEnabledStatus)
                    Log.i(TAG, "✅ Updated filter ${filter.name} enabled status to: $newEnabledStatus")

                    // Re-sync SMS messages to apply filter changes
                    triggerSmsResync("Toggled filter: ${filter.name} -> $newEnabledStatus")
                } else {
                    Log.w(TAG, "❌ Filter not found: $filterId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to toggle filter enabled status: $filterId", e)
            }
        }
    }

    fun deleteFilter(filterId: String) {
        Log.i(TAG, "Deleting sender filter: $filterId")
        viewModelScope.launch {
            try {
                val filter = senderFilterRepository.getSenderFilterById(filterId)
                if (filter != null) {
                    senderFilterRepository.deleteSenderFilterById(filterId)
                    Log.i(TAG, "✅ Successfully deleted sender filter: ${filter.name}")

                    // Re-sync SMS messages to apply filter removal
                    triggerSmsResync("Deleted filter: ${filter.name}")
                } else {
                    Log.w(TAG, "❌ Filter not found: $filterId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to delete sender filter: $filterId", e)
            }
        }
    }

    fun updateFilter(filter: SenderFilter) {
        Log.i(TAG, "Updating sender filter: ${filter.name}")
        viewModelScope.launch {
            try {
                senderFilterRepository.updateSenderFilter(filter)
                Log.i(TAG, "✅ Successfully updated sender filter: ${filter.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update sender filter: ${filter.name}", e)
            }
        }
    }

    private fun triggerSmsResync(
        reason: String,
        showNotification: Boolean = false,
        filterName: String? = null,
        filterPattern: String? = null
    ) {
        Log.i(TAG, "=== TRIGGERING SMS RE-SYNC ===")
        Log.i(TAG, "Reason: $reason")
        Log.i(TAG, "This will update ALL existing SMS messages with new filter matching")

        var updatedSmsCount = 0

        viewModelScope.launch {
            try {
                // Show sync progress notification
                notificationService.showSyncProgressNotification(0, 100, isUpdatingExisting = true)

                simpleSmsSync.syncSmsMessages(
                    progressCallback = { progress ->
                        val percentage = (progress * 100).toInt()
                        Log.i(TAG, "SMS re-sync progress: $percentage%")

                        if (percentage <= 50) {
                            Log.d(TAG, "  Phase 1: Updating existing SMS messages...")
                            notificationService.showSyncProgressNotification(percentage, 100, isUpdatingExisting = true)
                        } else {
                            Log.d(TAG, "  Phase 2: Processing new SMS messages...")
                            notificationService.showSyncProgressNotification(percentage, 100, isUpdatingExisting = false)
                        }
                    },
                    fullSync = true // Full sync to reprocess all SMS with new filters
                )

                // Hide sync progress notification
                notificationService.hideSyncProgressNotification()

                Log.i(TAG, "✅ SMS re-sync completed successfully")
                Log.i(TAG, "All existing SMS messages have been updated with new filter matching")

                // Show filter added notification if requested
                if (showNotification && filterName != null && filterPattern != null) {
                    // TODO: Get actual updated SMS count from sync result
                    notificationService.showFilterAddedNotification(filterName, filterPattern, updatedSmsCount)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ SMS re-sync failed", e)
                notificationService.hideSyncProgressNotification()
                notificationService.showErrorNotification("SMS Sync Failed", "Failed to update SMS with new filters", e)
            }
        }
    }
}
