package dev.albertus.expensms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.albertus.expensms.ui.screens.MainScreen
import dev.albertus.expensms.ui.screens.PermissionScreen
import dev.albertus.expensms.ui.screens.SettingsScreen
import dev.albertus.expensms.ui.theme.ExpenSMSTheme
import dev.albertus.expensms.ui.viewModels.MainViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var permissionState: MutableState<Boolean>
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionState.value = isGranted
        if (isGranted) {
            viewModel.loadSmsMessages()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpenSMSTheme {
                permissionState = remember { mutableStateOf(checkPermission()) }
                val navController = rememberNavController()

                Scaffold { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = if (permissionState.value) "main" else "permission",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable("permission") {
                            PermissionScreen(
                                onRequestPermission = { requestSmsPermission() }
                            )
                        }
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
    }
}