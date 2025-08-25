package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ApiConfigurationCard(
    apiForwardingEnabled: Boolean,
    apiEmail: String,
    apiPassword: String,
    apiTestResult: String?,
    isTestingApi: Boolean,
    onApiForwardingToggle: (Boolean) -> Unit,
    onApiEmailChange: (String) -> Unit,
    onApiPasswordChange: (String) -> Unit,
    onTestApiConnection: () -> Unit,
    onClearApiTestResult: () -> Unit,
    onClearApiConfiguration: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "API Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // API Forwarding Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable API Forwarding",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = apiForwardingEnabled,
                    onCheckedChange = onApiForwardingToggle
                )
            }

            if (apiForwardingEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // API Email Input
                OutlinedTextField(
                    value = apiEmail,
                    onValueChange = onApiEmailChange,
                    label = { Text("API Email") },
                    placeholder = { Text("your-email@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // API Password Input
                var passwordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = apiPassword,
                    onValueChange = onApiPasswordChange,
                    label = { Text("API Password") },
                    placeholder = { Text("Enter your password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Test Connection Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTestApiConnection,
                        enabled = !isTestingApi,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingApi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test Connection")
                        }
                    }

                    OutlinedButton(
                        onClick = onClearApiConfiguration,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Config")
                    }
                }

                // Test Result
                apiTestResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.startsWith("✅")) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearApiTestResult) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: Configure your aiccountant.id credentials in the app settings to enable automatic SMS forwarding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
