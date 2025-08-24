package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.ui.viewModels.MainViewModel
import dev.albertus.expensms.data.SupportedBank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val enabledBanks by viewModel.enabledBanks.collectAsState()
    val showMonthlyTotal by viewModel.showMonthlyTotal.collectAsState()
    val apiForwardingEnabled by viewModel.apiForwardingEnabled.collectAsState()
    val apiEmail by viewModel.apiEmail.collectAsState()
    val apiTestResult by viewModel.apiTestResult.collectAsState()
    val isTestingApi by viewModel.isTestingApi.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Supported Banks",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            SupportedBank.values().forEach { bank ->
                BankSettingItem(
                    bank = bank,
                    isEnabled = enabledBanks[bank.name] ?: true,
                    onEnabledChanged = { enabled ->
                        viewModel.setEnabledBank(bank, enabled)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show Monthly Total Spending",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = showMonthlyTotal,
                    onCheckedChange = viewModel::setShowMonthlyTotal
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // API Configuration Section
            ApiConfigurationSection(
                apiForwardingEnabled = apiForwardingEnabled,
                apiEmail = apiEmail,
                apiTestResult = apiTestResult,
                isTestingApi = isTestingApi,
                onApiForwardingEnabledChanged = viewModel::setApiForwardingEnabled,
                onTestConnection = viewModel::testApiConnection,
                onClearTestResult = viewModel::clearApiTestResult,
                onClearConfiguration = viewModel::clearApiConfiguration
            )
        }
    }
}

@Composable
fun BankSettingItem(
    bank: SupportedBank,
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bank.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChanged
            )
        }
        Text(
            text = "Sample SMS:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = bank.sampleSms,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ApiConfigurationSection(
    apiForwardingEnabled: Boolean,
    apiEmail: String,
    apiTestResult: String?,
    isTestingApi: Boolean,
    onApiForwardingEnabledChanged: (Boolean) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onClearTestResult: () -> Unit,
    onClearConfiguration: () -> Unit
) {
    var email by remember { mutableStateOf(apiEmail) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Update email when apiEmail changes
    LaunchedEffect(apiEmail) {
        email = apiEmail
    }

    Text(
        text = "API Integration",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(16.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Forward SMS to AI Accountant",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = apiForwardingEnabled,
            onCheckedChange = onApiForwardingEnabledChanged
        )
    }

    if (apiForwardingEnabled) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onClearTestResult()
                },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onClearTestResult()
                },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            onTestConnection(email, password)
                        }
                    },
                    enabled = !isTestingApi && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTestingApi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isTestingApi) "Testing..." else "Test Connection")
                }

                if (apiEmail.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearConfiguration,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }
                }
            }

            apiTestResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.startsWith("✓"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "When enabled, OCBC SMS messages will be automatically forwarded to your AI Accountant for processing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}