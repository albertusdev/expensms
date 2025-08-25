package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.albertus.expensms.data.model.SenderFilter
import dev.albertus.expensms.ui.viewModels.SenderFiltersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderFiltersScreen(
    onNavigateBack: () -> Unit,
    viewModel: SenderFiltersViewModel = hiltViewModel()
) {
    val senderFilters by viewModel.senderFilters.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Sender Filters") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasUnsavedChanges) {
                        TextButton(onClick = { showDiscardDialog = true }) {
                            Text("Discard")
                        }
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Filter")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Description
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "SMS Sender Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configure which SMS senders should be forwarded to the API. Only SMS messages from enabled senders will be processed and forwarded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Filters list
            if (senderFilters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No sender filters configured",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add filters to start forwarding SMS messages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(senderFilters) { filter ->
                        // Determine filter status for visual indicators
                        val originalFilters = viewModel.originalFilters.collectAsState().value
                        val localChanges = viewModel.changesMap.collectAsState().value
                        val pendingAdditions = viewModel.additionsMap.collectAsState().value
                        val pendingDeletions = viewModel.deletionsSet.collectAsState().value

                        val isNewFilter = pendingAdditions.any { it.id == filter.id }
                        val isMarkedForDeletion = pendingDeletions.contains(filter.id)
                        val originalFilter = originalFilters.find { it.id == filter.id }
                        val isModified = localChanges.containsKey(filter.id) && originalFilter != null

                        SenderFilterItem(
                            filter = filter,
                            onToggleEnabled = { viewModel.toggleFilterEnabled(filter.id) },
                            onDelete = { viewModel.deleteFilter(filter.id) },
                            isNewFilter = isNewFilter,
                            isModified = isModified,
                            isMarkedForDeletion = isMarkedForDeletion
                        )
                    }
                }
            }

            // Save/Apply Changes Button (sticky at bottom)
            if (hasUnsavedChanges) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unsaved Changes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Save to apply changes and sync SMS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = { viewModel.saveChanges() },
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }

    // Add filter dialog
    if (showAddDialog) {
        AddSenderFilterDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, pattern ->
                viewModel.addFilter(name, pattern)
                showAddDialog = false
            }
        )
    }

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("All unsaved changes will be lost. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.discardChanges()
                        showDiscardDialog = false
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SenderFilterItem(
    filter: SenderFilter,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    isNewFilter: Boolean = false,
    isModified: Boolean = false,
    isMarkedForDeletion: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isMarkedForDeletion -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                isNewFilter -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                isModified -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filter.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        when {
                            isMarkedForDeletion -> {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DELETED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            isNewFilter -> {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            isModified -> {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MODIFIED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Pattern: ${filter.filterPattern}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = filter.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddSenderFilterDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sender Filter") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Filter Name") },
                    placeholder = { Text("e.g., OCBC Bank") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Sender Pattern") },
                    placeholder = { Text("e.g., OCBC") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The pattern will match any SMS sender containing this text (case-insensitive).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name.trim(), pattern.trim()) },
                enabled = name.trim().isNotEmpty() && pattern.trim().isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
