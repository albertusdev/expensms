package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.utils.DateUtils.formatFullDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsMessageDetailScreen(
    smsMessage: SmsMessage,
    onNavigateBack: () -> Unit,
    onForceForward: ((SmsMessage) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Show force forward button for bank SMS that haven't been forwarded
                    if (smsMessage.bankSource != null && !smsMessage.isForwarded && onForceForward != null) {
                        IconButton(onClick = { onForceForward(smsMessage) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Forward to API"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SmsDetailsCard(smsMessage)
            Spacer(modifier = Modifier.height(16.dp))
            SmsContentCard(smsMessage.rawMessage)

            // Show force forward button as a card for bank SMS that haven't been forwarded
            if (smsMessage.bankSource != null && !smsMessage.isForwarded && onForceForward != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ForceForwardSmsCard(smsMessage, onForceForward)
            }
        }
    }
}

@Composable
fun SmsDetailsCard(smsMessage: SmsMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SMS Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow("Sender", smsMessage.sender)
            if (smsMessage.bankSource != null) {
                DetailRow("Bank", smsMessage.bankSource)
            }
            DetailRow("Received", smsMessage.timestamp.formatFullDateTime())
            DetailRow("Status", if (smsMessage.isForwarded) "Forwarded" else "Not forwarded")

            if (smsMessage.isForwarded && smsMessage.forwardedAt != null) {
                DetailRow("Forwarded At", smsMessage.forwardedAt.formatFullDateTime())
            }
        }
    }
}

@Composable
fun SmsContentCard(rawMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SMS Content",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = rawMessage,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ForceForwardSmsCard(smsMessage: SmsMessage, onForceForward: (SmsMessage) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Forward to API",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This SMS can be forwarded to the aiccountant API for processing.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onForceForward(smsMessage) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Forward SMS")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}
