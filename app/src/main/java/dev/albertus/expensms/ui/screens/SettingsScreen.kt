package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.albertus.expensms.ui.components.ApiConfigurationCard
import dev.albertus.expensms.ui.viewModels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiForwardingEnabled by viewModel.apiForwardingEnabled.collectAsState()
    val apiEmail by viewModel.apiEmail.collectAsState()
    val apiPassword by viewModel.apiPassword.collectAsState()
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
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SMS Forwarding Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure SMS sender filters and API settings below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ApiConfigurationCard(
                    apiForwardingEnabled = apiForwardingEnabled,
                    apiEmail = apiEmail,
                    apiPassword = apiPassword,
                    apiTestResult = apiTestResult,
                    isTestingApi = isTestingApi,
                    onApiForwardingToggle = viewModel::setApiForwardingEnabled,
                    onApiEmailChange = viewModel::setApiEmail,
                    onApiPasswordChange = viewModel::setApiPassword,
                    onTestApiConnection = viewModel::testApiConnection,
                    onClearApiTestResult = viewModel::clearApiTestResult,
                    onClearApiConfiguration = viewModel::clearApiConfiguration
                )
            }
        }
    }
}
