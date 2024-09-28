package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderFilterInput(senderFilter: String, onSenderFilterChanged: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = senderFilter,
            onValueChange = onSenderFilterChanged,
            label = { Text("Sender Filter") },
            placeholder = { Text("e.g., OCBC, DBS, UOB") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text("Enter the sender name or part of it to filter SMS messages.")
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This filter helps to narrow down the SMS messages parsed for transactions. " +
                    "Enter the name of your bank or any other identifier used in transaction SMS messages.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}