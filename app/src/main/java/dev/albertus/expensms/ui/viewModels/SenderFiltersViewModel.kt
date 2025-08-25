package dev.albertus.expensms.ui.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.albertus.expensms.data.model.SenderFilter
import dev.albertus.expensms.data.repository.SenderFilterRepository
import dev.albertus.expensms.utils.NotificationService
import dev.albertus.expensms.utils.SimpleSmsSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    // Original filters from database
    private val originalSenderFilters: StateFlow<List<SenderFilter>> = senderFilterRepository.getAllSenderFiltersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Local state for pending changes
    private val _localChanges = MutableStateFlow<Map<String, SenderFilter>>(emptyMap())
    private val localChanges = _localChanges.asStateFlow()

    // Pending additions (not yet saved to database)
    private val _pendingAdditions = MutableStateFlow<List<SenderFilter>>(emptyList())
    private val pendingAdditions = _pendingAdditions.asStateFlow()

    // Pending deletions (IDs of filters to delete)
    private val _pendingDeletions = MutableStateFlow<Set<String>>(emptySet())
    private val pendingDeletions = _pendingDeletions.asStateFlow()

    // Combined state: original + local changes + pending additions - pending deletions
    val senderFilters: StateFlow<List<SenderFilter>> = combine(
        originalSenderFilters,
        localChanges,
        pendingAdditions,
        pendingDeletions
    ) { original, changes, additions, deletions ->
        // Start with original filters, excluding deleted ones
        val result = original
            .filter { !deletions.contains(it.id) }
            .map { filter -> changes[filter.id] ?: filter }
            .toMutableList()

        // Add pending additions
        result.addAll(additions)

        result.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Check if there are any unsaved changes
    val hasUnsavedChanges: StateFlow<Boolean> = combine(
        localChanges,
        pendingAdditions,
        pendingDeletions
    ) { changes, additions, deletions ->
        changes.isNotEmpty() || additions.isNotEmpty() || deletions.isNotEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Expose internal state for UI status indicators
    val originalFilters: StateFlow<List<SenderFilter>> get() = originalSenderFilters
    val changesMap: StateFlow<Map<String, SenderFilter>> get() = _localChanges.asStateFlow()
    val additionsMap: StateFlow<List<SenderFilter>> get() = _pendingAdditions.asStateFlow()
    val deletionsSet: StateFlow<Set<String>> get() = _pendingDeletions.asStateFlow()

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
        Log.i(TAG, "Adding new sender filter locally: $name -> $pattern")
        val filter = SenderFilter(
            id = UUID.randomUUID().toString(),
            name = name,
            filterPattern = pattern,
            isEnabled = true
        )

        // Add to pending additions (local state only)
        val currentAdditions = _pendingAdditions.value.toMutableList()
        currentAdditions.add(filter)
        _pendingAdditions.value = currentAdditions

        Log.i(TAG, "✅ Added sender filter to pending changes: $name")
        Log.i(TAG, "💾 Remember to save changes to apply this filter")
    }

    fun toggleFilterEnabled(filterId: String) {
        Log.i(TAG, "Toggling enabled status locally for filter: $filterId")

        // Find the current filter (from combined state)
        val currentFilter = senderFilters.value.find { it.id == filterId }
        if (currentFilter != null) {
            val updatedFilter = currentFilter.copy(isEnabled = !currentFilter.isEnabled)

            // Update local changes
            val currentChanges = _localChanges.value.toMutableMap()
            currentChanges[filterId] = updatedFilter
            _localChanges.value = currentChanges

            Log.i(TAG, "✅ Updated filter ${currentFilter.name} enabled status locally to: ${updatedFilter.isEnabled}")
            Log.i(TAG, "💾 Remember to save changes to apply this change")
        } else {
            Log.w(TAG, "❌ Filter not found: $filterId")
        }
    }

    fun deleteFilter(filterId: String) {
        Log.i(TAG, "Marking sender filter for deletion: $filterId")

        // Check if it's a pending addition (not yet saved)
        val currentAdditions = _pendingAdditions.value.toMutableList()
        val pendingFilter = currentAdditions.find { it.id == filterId }

        if (pendingFilter != null) {
            // Remove from pending additions
            currentAdditions.removeAll { it.id == filterId }
            _pendingAdditions.value = currentAdditions
            Log.i(TAG, "✅ Removed pending filter: ${pendingFilter.name}")
        } else {
            // Mark existing filter for deletion
            val currentDeletions = _pendingDeletions.value.toMutableSet()
            currentDeletions.add(filterId)
            _pendingDeletions.value = currentDeletions

            // Remove from local changes if it exists
            val currentChanges = _localChanges.value.toMutableMap()
            currentChanges.remove(filterId)
            _localChanges.value = currentChanges

            Log.i(TAG, "✅ Marked filter for deletion: $filterId")
        }

        Log.i(TAG, "💾 Remember to save changes to apply this deletion")
    }

    fun updateFilter(filter: SenderFilter) {
        Log.i(TAG, "Updating sender filter locally: ${filter.name}")

        // Update local changes
        val currentChanges = _localChanges.value.toMutableMap()
        currentChanges[filter.id] = filter
        _localChanges.value = currentChanges

        Log.i(TAG, "✅ Updated sender filter locally: ${filter.name}")
        Log.i(TAG, "💾 Remember to save changes to apply this update")
    }

    fun saveChanges() {
        Log.i(TAG, "=== SAVING ALL PENDING CHANGES ===")
        viewModelScope.launch {
            try {
                val changes = _localChanges.value
                val additions = _pendingAdditions.value
                val deletions = _pendingDeletions.value

                Log.i(TAG, "Changes to apply:")
                Log.i(TAG, "  - ${changes.size} filter updates")
                Log.i(TAG, "  - ${additions.size} new filters")
                Log.i(TAG, "  - ${deletions.size} filter deletions")

                // Apply deletions first
                deletions.forEach { filterId ->
                    val filter = senderFilterRepository.getSenderFilterById(filterId)
                    if (filter != null) {
                        senderFilterRepository.deleteSenderFilterById(filterId)
                        Log.i(TAG, "✅ Deleted filter: ${filter.name}")
                    }
                }

                // Apply updates
                changes.values.forEach { filter ->
                    senderFilterRepository.updateSenderFilter(filter)
                    Log.i(TAG, "✅ Updated filter: ${filter.name}")
                }

                // Apply additions
                additions.forEach { filter ->
                    senderFilterRepository.insertSenderFilter(filter)
                    Log.i(TAG, "✅ Added filter: ${filter.name}")
                }

                // Clear all pending changes
                _localChanges.value = emptyMap()
                _pendingAdditions.value = emptyList()
                _pendingDeletions.value = emptySet()

                Log.i(TAG, "✅ All changes saved successfully")

                // Trigger SMS resync to apply all changes
                triggerSmsResync("Applied ${changes.size + additions.size + deletions.size} filter changes")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save changes", e)
            }
        }
    }

    fun discardChanges() {
        Log.i(TAG, "=== DISCARDING ALL PENDING CHANGES ===")
        val changes = _localChanges.value.size
        val additions = _pendingAdditions.value.size
        val deletions = _pendingDeletions.value.size

        _localChanges.value = emptyMap()
        _pendingAdditions.value = emptyList()
        _pendingDeletions.value = emptySet()

        Log.i(TAG, "✅ Discarded ${changes + additions + deletions} pending changes")
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
