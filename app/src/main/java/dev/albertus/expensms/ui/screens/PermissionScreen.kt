package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Permissions Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "This app needs the following permissions to function properly:",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // SMS Permission
                PermissionItem(
                    title = "SMS Permissions",
                    description = "Required to read existing SMS and receive new SMS messages in background for automatic processing and forwarding",
                    isGranted = hasSmsPermission,
                    onRequest = onRequestSmsPermission
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notification Permission
                PermissionItem(
                    title = "Notification Permission",
                    description = "Required to show notifications for background SMS processing",
                    isGranted = hasNotificationPermission,
                    onRequest = onRequestNotificationPermission
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isGranted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}