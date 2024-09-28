package dev.albertus.expensms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import dev.albertus.expensms.ui.screens.MainScreen
import dev.albertus.expensms.ui.screens.PermissionScreen
import dev.albertus.expensms.ui.theme.ExpenSMSTheme
import dev.albertus.expensms.ui.viewModels.MainViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenSMSTheme {
                permissionState = remember { mutableStateOf(checkPermission()) }
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (permissionState.value) {
                        MainScreen(viewModel)
                    } else {
                        PermissionScreen(
                            onRequestPermission = { requestSmsPermission() }
                        )
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