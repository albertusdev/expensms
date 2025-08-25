package dev.albertus.expensms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.ui.screens.ApiLogsScreen
import dev.albertus.expensms.ui.screens.ErrorScreen
import dev.albertus.expensms.ui.screens.PermissionScreen
import dev.albertus.expensms.ui.screens.SenderFiltersScreen
import dev.albertus.expensms.ui.screens.SettingsScreen
import dev.albertus.expensms.ui.screens.SmsMainScreen
import dev.albertus.expensms.ui.screens.SmsMessageDetailScreen
import dev.albertus.expensms.ui.viewModels.ApiLogsViewModel
import dev.albertus.expensms.ui.theme.ExpenSMSTheme
import dev.albertus.expensms.ui.viewModels.SmsMainViewModel
import dev.albertus.expensms.utils.SimpleSmsForwardingService
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var smsPermissionState: MutableState<Boolean>
    private lateinit var notificationPermissionState: MutableState<Boolean>
    private val smsMainViewModel: SmsMainViewModel by viewModels()
    private val apiLogsViewModel: ApiLogsViewModel by viewModels()

    @Inject
    lateinit var simpleSmsForwardingService: SimpleSmsForwardingService

    private val requestSmsPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readSmsGranted = permissions[Manifest.permission.READ_SMS] ?: false
        val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val allSmsPermissionsGranted = readSmsGranted && receiveSmsGranted

        smsPermissionState.value = allSmsPermissionsGranted

        Log.i("MainActivity", "SMS Permissions Result:")
        Log.i("MainActivity", "  READ_SMS: $readSmsGranted")
        Log.i("MainActivity", "  RECEIVE_SMS: $receiveSmsGranted")
        Log.i("MainActivity", "  All granted: $allSmsPermissionsGranted")

        if (allSmsPermissionsGranted) {
            // Trigger SMS sync when all permissions are granted
            smsMainViewModel.syncSmsMessages(fullSync = true)
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        notificationPermissionState.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenSMSTheme {
                smsPermissionState = remember { mutableStateOf(checkSmsPermission()) }
                notificationPermissionState = remember { mutableStateOf(checkNotificationPermission()) }
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(12.dp))
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Home") },
                                selected = false,
                                onClick = {
                                    navController.navigate("main") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                }
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                label = { Text("API Logs") },
                                selected = false,
                                onClick = {
                                    navController.navigate("apiLogs") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                }
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                                label = { Text("Sender Filters") },
                                selected = false,
                                onClick = {
                                    navController.navigate("senderFilters") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                }
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Settings") },
                                selected = false,
                                onClick = {
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                ) {
                    NavHost(
                        navController,
                        startDestination = if (smsPermissionState.value && notificationPermissionState.value) "main" else "permission",
                    ) {
                        composable("permission") {
                            PermissionScreen(
                                hasSmsPermission = smsPermissionState.value,
                                hasNotificationPermission = notificationPermissionState.value,
                                onRequestSmsPermission = { requestSmsPermission() },
                                onRequestNotificationPermission = { requestNotificationPermission() }
                            )
                        }
                        composable(
                            route = "main",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                        ) {
                            SmsMainScreen(
                                onNavigateToSmsDetail = { id -> navController.navigate("smsDetail/$id") },
                                drawerState = drawerState
                            )
                        }
                        composable(
                            route = "settings",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                        ) {
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "smsDetail/{smsMessageId}",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                        ) { backStackEntry ->
                            val smsMessageId = backStackEntry.arguments?.getString("smsMessageId")
                            Log.d("MainActivity", "Looking for SMS with ID: $smsMessageId")

                            // Use a state to hold the SMS message from database if needed
                            var smsMessage by remember { mutableStateOf<SmsMessage?>(null) }
                            var isLoading by remember { mutableStateOf(true) }

                            // First try to get from StateFlow
                            val smsFromStateFlow = smsMainViewModel.getSmsMessageById(smsMessageId)
                            Log.d("MainActivity", "Found SMS in StateFlow: ${smsFromStateFlow?.id}")

                            // If found in StateFlow, use it immediately
                            if (smsFromStateFlow != null) {
                                smsMessage = smsFromStateFlow
                                isLoading = false
                            } else if (smsMessageId != null) {
                                // If not found in StateFlow, try database directly (for timing issues)
                                Log.d("MainActivity", "SMS not found in StateFlow, checking database...")
                                LaunchedEffect(smsMessageId) {
                                    val smsFromDb = smsMainViewModel.getSmsMessageByIdFromDb(smsMessageId)
                                    Log.d("MainActivity", "Found SMS in database: ${smsFromDb?.id}")
                                    smsMessage = smsFromDb
                                    isLoading = false
                                }
                            } else {
                                isLoading = false
                            }

                            when {
                                isLoading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                smsMessage != null -> {
                                    val currentSms = smsMessage!!
                                    SmsMessageDetailScreen(
                                        smsMessage = currentSms,
                                        onNavigateBack = { navController.popBackStack() },
                                        onForceForward = { smsMessageToForward ->
                                            simpleSmsForwardingService.forwardSmsMessageIfEnabled(smsMessageToForward)
                                        }
                                    )
                                }
                                else -> {
                                    ErrorScreen(
                                        message = "SMS message not found",
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                        composable(
                            route = "senderFilters",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                        ) {
                            SenderFiltersScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "apiLogs",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                        ) {
                            ApiLogsScreen(
                                viewModel = apiLogsViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToSmsDetail = { id -> navController.navigate("smsDetail/$id") }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkSmsPermission(): Boolean {
        val readSmsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val receiveSmsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val allGranted = readSmsGranted && receiveSmsGranted

        Log.d("MainActivity", "SMS Permissions Check:")
        Log.d("MainActivity", "  READ_SMS: $readSmsGranted")
        Log.d("MainActivity", "  RECEIVE_SMS: $receiveSmsGranted")
        Log.d("MainActivity", "  All granted: $allGranted")

        return allGranted
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Notification permission not required for older versions
        }
    }

    private fun requestSmsPermission() {
        Log.i("MainActivity", "Requesting SMS permissions (READ_SMS + RECEIVE_SMS)")
        requestSmsPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            )
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
