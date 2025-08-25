package dev.albertus.expensms.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.ui.components.SmsMessageItem
import dev.albertus.expensms.ui.components.TransactionCalendar
import dev.albertus.expensms.ui.model.SelectionMode
import dev.albertus.expensms.ui.utils.rememberFoldableInfo
import dev.albertus.expensms.ui.utils.shouldUseTwoPaneLayout
import dev.albertus.expensms.ui.viewModels.SmsMainViewModel
import dev.albertus.expensms.utils.DateUtils.formatToReadable
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsMainScreen(
    onNavigateToSmsDetail: (String) -> Unit,
    drawerState: DrawerState,
    viewModel: SmsMainViewModel = hiltViewModel()
) {
    val filteredSmsMessages by viewModel.filteredSmsMessages.collectAsState()
    val selectedSmsMessages by viewModel.selectedSmsMessages.collectAsState()
    val deleteMode by viewModel.deleteMode.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Use fold-aware layout detection
    val foldableInfo = rememberFoldableInfo()
    val isWideScreen = shouldUseTwoPaneLayout()

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showForwardConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Get bank count for forward button
    val bankCount = remember(selectedSmsMessages) {
        selectedSmsMessages.count { id ->
            filteredSmsMessages.values.flatten().find { it.id == id }?.let { sms ->
                sms.sender.contains("OCBC", ignoreCase = true) ||
                sms.sender.contains("UOB", ignoreCase = true)
            } ?: false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (selectionMode) {
                        SelectionMode.DELETE -> Text("${selectedSmsMessages.size} selected")
                        SelectionMode.MULTI_SELECT -> Text("${selectedSmsMessages.size} selected")
                        else -> Text("SMS Messages")
                    }
                },
                navigationIcon = {
                    if (selectionMode != SelectionMode.NONE) {
                        IconButton(onClick = { viewModel.setSelectionMode(SelectionMode.NONE) }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection mode")
                        }
                    } else {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                },
                actions = {
                    if (selectionMode == SelectionMode.DELETE) {
                        // Select all/deselect all button
                        val allSelected = selectedSmsMessages.size == filteredSmsMessages.values.flatten().size
                        IconButton(onClick = {
                            if (allSelected) {
                                viewModel.deselectAllSmsMessages()
                            } else {
                                viewModel.selectAllFilteredSmsMessages()
                            }
                        }) {
                            Icon(
                                if (allSelected) Icons.Default.CheckBoxOutlineBlank else Icons.Default.CheckBox,
                                contentDescription = if (allSelected) "Deselect all" else "Select all"
                            )
                        }

                        // Forward button (only show if bank SMS messages are selected)
                        if (bankCount > 0) {
                            IconButton(onClick = { showForwardConfirmation = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Forward selected bank SMS messages"
                                )
                            }
                        }

                        // Mark as forwarded button
                        IconButton(onClick = { viewModel.markSelectedAsForwarded() }) {
                            Icon(Icons.Default.Check, contentDescription = "Mark as forwarded")
                        }

                        // Mark as not forwarded button
                        IconButton(onClick = { viewModel.markSelectedAsNotForwarded() }) {
                            Icon(Icons.Default.Close, contentDescription = "Mark as not forwarded")
                        }

                        // Delete button
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    } else if (selectionMode == SelectionMode.MULTI_SELECT) {
                        // Multi-select mode actions
                        // Select all/deselect all button
                        val allSelected = selectedSmsMessages.size == filteredSmsMessages.values.flatten().size
                        IconButton(onClick = {
                            if (allSelected) {
                                viewModel.deselectAllSmsMessages()
                            } else {
                                viewModel.selectAllFilteredSmsMessages()
                            }
                        }) {
                            Icon(
                                if (allSelected) Icons.Default.CheckBoxOutlineBlank else Icons.Default.CheckBox,
                                contentDescription = if (allSelected) "Deselect all" else "Select all"
                            )
                        }

                        // Forward button (only show if bank SMS messages are selected)
                        if (bankCount > 0) {
                            IconButton(onClick = { showForwardConfirmation = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Forward selected bank SMS messages"
                                )
                            }
                        }

                        // Mark as forwarded button
                        IconButton(onClick = { viewModel.markSelectedAsForwarded() }) {
                            Icon(Icons.Default.Check, contentDescription = "Mark as forwarded")
                        }

                        // Mark as not forwarded button
                        IconButton(onClick = { viewModel.markSelectedAsNotForwarded() }) {
                            Icon(Icons.Default.Close, contentDescription = "Mark as not forwarded")
                        }

                        // Delete button
                        IconButton(onClick = { viewModel.setSelectionMode(SelectionMode.DELETE) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Switch to delete mode")
                        }
                    } else {
                        // Only keep essential actions in the app bar
                        IconButton(onClick = { viewModel.setDeleteMode(true) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete mode")
                        }
                        IconButton(onClick = { viewModel.refreshSmsMessages() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh SMS")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isWideScreen) {
            // Fold-aware two-pane layout
            Row(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                // Left pane: Calendar and sync indicator
                Column(
                    modifier = Modifier
                        .weight(
                            if (foldableInfo.isSeparating && foldableInfo.foldBounds != null) {
                                // Calculate weight based on fold position
                                val density = LocalDensity.current
                                val foldPositionDp: Dp = with(density) { foldableInfo.foldBounds!!.left.toDp() }
                                val screenWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp
                                val weight = (foldPositionDp.value / screenWidthDp.value).coerceIn(0.3f, 0.7f)
                                Log.d("SmsMainScreen", "Fold position: ${foldPositionDp}, Screen width: ${screenWidthDp}, Left weight: $weight")
                                weight
                            } else {
                                0.35f // Default weight for non-foldable devices
                            }
                        )
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sync indicator
                    if (isSyncing) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Syncing SMS...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Calendar header
                    SmsHeaderContent(
                        selectedDate = viewModel.selectedDate.collectAsState().value,
                        selectedMonth = viewModel.selectedMonth.collectAsState().value,
                        onDateSelected = viewModel::setSelectedDate,
                        onMonthSelected = viewModel::setSelectedMonth,
                        groupedSmsMessages = viewModel.groupedSmsMessages.collectAsState().value
                    )
                }

                // Add spacing for the hinge if present
                if (foldableInfo.isSeparating && foldableInfo.hingeWidthDp > 0) {
                    Spacer(modifier = Modifier.width(foldableInfo.hingeWidthDp.dp))
                }

                // Right pane: SMS list
                SmsListColumn(
                    filteredSmsMessages = filteredSmsMessages,
                    selectedSmsMessages = selectedSmsMessages,
                    selectionMode = selectionMode,
                    onNavigateToSmsDetail = onNavigateToSmsDetail,
                    onToggleSelection = { sms -> viewModel.toggleSmsMessageSelection(sms.id) },
                    onLongPress = { sms ->
                        if (selectionMode == SelectionMode.NONE) {
                            viewModel.enterSelectionModeAndSelect(sms.id)
                        }
                    },
                    modifier = Modifier
                        .weight(
                            if (foldableInfo.isSeparating && foldableInfo.foldBounds != null) {
                                // Calculate remaining weight for right pane
                                val density = LocalDensity.current
                                val foldPositionDp: Dp = with(density) { foldableInfo.foldBounds!!.left.toDp() }
                                val screenWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp
                                val leftWeight = (foldPositionDp.value / screenWidthDp.value).coerceIn(0.3f, 0.7f)
                                val rightWeight = 1f - leftWeight
                                Log.d("SmsMainScreen", "Right weight: $rightWeight")
                                rightWeight
                            } else {
                                0.65f // Default weight for non-foldable devices
                            }
                        )
                        .padding(end = 16.dp)
                )
            }
        } else {
            // Narrow screen layout: Vertical stack
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                // Sync indicator
                if (isSyncing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Syncing SMS messages...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Header with date picker
                SmsHeaderContent(
                    selectedDate = viewModel.selectedDate.collectAsState().value,
                    selectedMonth = viewModel.selectedMonth.collectAsState().value,
                    onDateSelected = viewModel::setSelectedDate,
                    onMonthSelected = viewModel::setSelectedMonth,
                    groupedSmsMessages = viewModel.groupedSmsMessages.collectAsState().value
                )

                // SMS messages list
                SmsListColumn(
                    filteredSmsMessages = filteredSmsMessages,
                    selectedSmsMessages = selectedSmsMessages,
                    selectionMode = selectionMode,
                    onNavigateToSmsDetail = onNavigateToSmsDetail,
                    onToggleSelection = { sms -> viewModel.toggleSmsMessageSelection(sms.id) },
                    onLongPress = { sms ->
                        if (selectionMode == SelectionMode.NONE) {
                            viewModel.enterSelectionModeAndSelect(sms.id)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete SMS Messages") },
            text = { Text("Are you sure you want to delete ${selectedSmsMessages.size} SMS message(s)? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedSmsMessages()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Forward confirmation dialog
    if (showForwardConfirmation) {
        AlertDialog(
            onDismissRequest = { showForwardConfirmation = false },
            title = { Text("Forward SMS Messages") },
            text = { Text("Forward $bankCount bank SMS message(s) to the aiccountant API for processing?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.forwardSelectedSmsMessages()
                        showForwardConfirmation = false
                    }
                ) {
                    Text("Forward")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForwardConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SmsListColumn(
    filteredSmsMessages: Map<LocalDate, List<SmsMessage>>,
    selectedSmsMessages: Set<String>,
    selectionMode: SelectionMode,
    onNavigateToSmsDetail: (String) -> Unit,
    onToggleSelection: (SmsMessage) -> Unit,
    onLongPress: (SmsMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (filteredSmsMessages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No SMS messages found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "SMS messages from banks will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier) {
            filteredSmsMessages.forEach { (date, smsMessagesForDate) ->
                item {
                    DateHeader(date = date)
                }
                items(smsMessagesForDate) { smsMessage ->
                    SmsMessageItem(
                        smsMessage = smsMessage,
                        onClick = { onNavigateToSmsDetail(smsMessage.id) },
                        selectionMode = selectionMode,
                        isSelected = smsMessage.id in selectedSmsMessages,
                        onSelect = { smsId -> onToggleSelection(smsMessage) },
                        onLongPress = { onLongPress(smsMessage) }
                    )
                }
            }
        }
    }
}

@Composable
fun SmsHeaderContent(
    selectedDate: LocalDate?,
    selectedMonth: java.time.YearMonth?,
    onDateSelected: (LocalDate?) -> Unit,
    onMonthSelected: (java.time.YearMonth?) -> Unit,
    groupedSmsMessages: Map<LocalDate, List<SmsMessage>>
) {
    TransactionCalendar(
        availableDates = groupedSmsMessages.keys,
        onDateSelected = onDateSelected,
        selectedDate = selectedDate,
        onMonthChanged = onMonthSelected,
        modifier = Modifier.fillMaxWidth(),
        transactionCounts = groupedSmsMessages.mapValues { it.value.size }
    )
}

@Composable
fun DateHeader(date: LocalDate) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = date.formatToReadable(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}