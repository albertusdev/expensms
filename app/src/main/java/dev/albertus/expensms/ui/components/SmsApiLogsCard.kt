package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.ApiLogWithSmsMessage
import dev.albertus.expensms.data.model.ApiLogType
import dev.albertus.expensms.utils.DateUtils.formatFullDateTime

@Composable
fun SmsApiLogsCard(
    apiLogs: List<ApiLogWithSmsMessage>,
    modifier: Modifier = Modifier
) {
    if (apiLogs.isEmpty()) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "API Forwarding History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Detailed logs of API calls for this SMS message",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(apiLogs) { apiLogWithSms ->
                    ApiLogItem(apiLogWithSms = apiLogWithSms)
                }
            }
        }
    }
}

@Composable
private fun ApiLogItem(
    apiLogWithSms: ApiLogWithSmsMessage
) {
    val apiLog = apiLogWithSms.apiLog
    val isSuccess = apiLog.responseCode in 200..299
    val isError = apiLog.errorMessage != null
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSuccess -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with status and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            isSuccess -> Icons.Default.CheckCircle
                            isError -> Icons.Default.Error
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when {
                            isSuccess -> MaterialTheme.colorScheme.primary
                            isError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isSuccess -> "Success"
                            isError -> "Failed"
                            else -> "Pending"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isSuccess -> MaterialTheme.colorScheme.primary
                            isError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                }
                
                Text(
                    text = apiLog.timestamp.formatFullDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // API details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${apiLog.httpMethod} ${apiLog.endpoint}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (apiLog.responseCode != null) {
                        Text(
                            text = "Response: ${apiLog.responseCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (apiLog.durationMs != null) {
                    Text(
                        text = "${apiLog.durationMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Error message if present
            if (apiLog.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Error: ${apiLog.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Response preview if available
            if (apiLog.responseBody != null && isSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Response: ${apiLog.responseBody.take(100)}${if (apiLog.responseBody.length > 100) "..." else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
