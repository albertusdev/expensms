package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderFilterInput(senderFilter: String, onSenderFilterChanged: (String) -> Unit) {
    OutlinedTextField(
        value = senderFilter,
        onValueChange = onSenderFilterChanged,
        label = { Text("Sender Filter") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}