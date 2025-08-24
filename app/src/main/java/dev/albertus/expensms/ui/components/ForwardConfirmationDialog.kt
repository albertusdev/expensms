package dev.albertus.expensms.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ForwardConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forward to API") },
        text = { 
            Text("Forward $count selected OCBC transaction${if (count != 1) "s" else ""} to the AI Accountant API?\n\nThis will create new API log entries for each transaction.") 
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Forward")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
