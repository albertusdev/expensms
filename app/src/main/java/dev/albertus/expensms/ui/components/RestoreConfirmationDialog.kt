package dev.albertus.expensms.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun RestoreConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Restore") },
        text = { Text("Are you sure you want to restore $count transaction(s)?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}