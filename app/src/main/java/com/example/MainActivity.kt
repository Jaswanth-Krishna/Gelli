package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GuardPrefs
import com.example.service.DeviceStatusHelper
import com.example.service.SafetyForegroundService
import com.example.ui.GuardScreen
import com.example.ui.GuardUiEvent
import com.example.ui.GuardViewModel
import com.example.ui.theme.FlashGetGuardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // If already paired and location permission is granted, ensure foreground service is running
        val existingCode = GuardPrefs.getPairingCode(this)
        if (!existingCode.isNullOrBlank() && DeviceStatusHelper.hasLocationPermission(this)) {
            SafetyForegroundService.start(this, existingCode)
        }

        setContent {
            FlashGetGuardTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                var pendingCodeForPermissions by remember { mutableStateOf<String?>(null) }

                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val code = pendingCodeForPermissions
                        ?: uiState.pairedCode
                        ?: GuardPrefs.getPairingCode(this@MainActivity)
                    if (!code.isNullOrBlank()) {
                        viewModel.onPermissionsResult(code, results)
                    }
                    pendingCodeForPermissions = null
                }

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is GuardUiEvent.RequestPermissionsAndStart -> {
                                pendingCodeForPermissions = event.code
                                permissionsLauncher.launch(
                                    DeviceStatusHelper.requiredRuntimePermissions()
                                )
                            }

                            is GuardUiEvent.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                        }
                    }
                }

                GuardScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onCodeChanged = viewModel::onCodeInputChanged,
                    onVerifyAndStart = viewModel::verifyCodeAndStartProtection,
                    onDisableBatteryOpt = {
                        DeviceStatusHelper.openBatteryOptimizationSettings(this@MainActivity)
                    },
                    onOpenAccessibility = {
                        DeviceStatusHelper.openAccessibilitySettings(this@MainActivity)
                    },
                    onOpenNotificationListener = {
                        DeviceStatusHelper.openNotificationListenerSettings(this@MainActivity)
                    },
                    onSendSos = viewModel::sendEmergencySos
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDeviceStatus()
    }
}
