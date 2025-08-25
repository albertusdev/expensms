package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.albertus.expensms.data.model.ApiLog
import dev.albertus.expensms.data.model.ApiLogType
import dev.albertus.expensms.data.model.ApiLogWithTransaction
import dev.albertus.expensms.ui.viewModels.ApiLogsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiLogsScreen(
    viewModel: ApiLogsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSmsDetail: (String) -> Unit
) {
    val apiLogs by viewModel.apiLogs.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear logs")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (apiLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No API logs yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "API calls will appear here when you use the forwarding feature",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apiLogs) { logWithTransaction ->
                    ApiLogItem(
                        logWithTransaction = logWithTransaction,
                        onNavigateToSmsDetail = onNavigateToSmsDetail
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear API Logs") },
            text = { Text("Are you sure you want to clear all API logs? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ApiLogItem(
    logWithTransaction: ApiLogWithTransaction,
    onNavigateToSmsDetail: (String) -> Unit
) {
    val log = logWithTransaction.apiLog
    val transaction = logWithTransaction.transaction
    var expanded by remember { mutableStateOf(false) }
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LogTypeChip(logType = log.logType)
                        StatusChip(
                            isSuccess = log.responseCode in 200..299,
                            responseCode = log.responseCode
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${log.httpMethod} ${log.endpoint}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dateFormatter.format(log.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    log.durationMs?.let { duration ->
                        Text(
                            text = "${duration}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Link to SMS detail if transaction exists
                    if (transaction != null) {
                        TextButton(
                            onClick = { onNavigateToSmsDetail(transaction.id) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "View SMS",
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Expanded content
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Request body
                    log.requestBody?.let { requestBody ->
                        LogSection(
                            title = "Request Body",
                            content = requestBody
                        )
                    }
                    
                    // Response body
                    log.responseBody?.let { responseBody ->
                        LogSection(
                            title = "Response Body",
                            content = responseBody
                        )
                    }
                    
                    // Error message
                    log.errorMessage?.let { errorMessage ->
                        LogSection(
                            title = "Error",
                            content = errorMessage,
                            isError = true
                        )
                    }
                    
                    // Transaction info
                    if (transaction != null) {
                        LogSection(
                            title = "Related Transaction",
                            content = "Bank: ${transaction.bank}\nMerchant: ${transaction.merchant}\nAmount: ${transaction.amount}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogTypeChip(logType: ApiLogType) {
    val color = when (logType) {
        ApiLogType.LOGIN -> MaterialTheme.colorScheme.primary
        ApiLogType.TOKEN_REFRESH -> MaterialTheme.colorScheme.secondary
        ApiLogType.SMS_FORWARD -> MaterialTheme.colorScheme.tertiary
        ApiLogType.SMS_BATCH_FORWARD -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
        ApiLogType.CONNECTION_TEST -> MaterialTheme.colorScheme.outline
    }
    
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = logType.name.replace("_", " "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}

@Composable
fun StatusChip(isSuccess: Boolean, responseCode: Int?) {
    val color = if (isSuccess) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = responseCode?.toString() ?: "ERROR",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}

@Composable
fun LogSection(
    title: String,
    content: String,
    isError: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            color = if (isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
