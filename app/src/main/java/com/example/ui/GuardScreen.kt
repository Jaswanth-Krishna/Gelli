package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GuardCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.OnGuardCyan
import com.example.ui.theme.OnSosCrimson
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.SosCrimson
import com.example.ui.theme.SosCrimsonContainer
import com.example.ui.theme.SosCrimsonDeep

@Composable
fun GuardScreen(
    uiState: GuardUiState,
    snackbarHostState: SnackbarHostState,
    onCodeChanged: (String) -> Unit,
    onVerifyAndStart: () -> Unit,
    onDisableBatteryOpt: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenNotificationListener: () -> Unit,
    onSendSos: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = ObsidianBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(color = ObsidianBackground)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GuardCyan.copy(alpha = 0.11f),
                                ObsidianBackground.copy(alpha = 0f)
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.08f),
                            radius = size.maxDimension * 0.55f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ShieldEmerald.copy(alpha = 0.08f),
                                ObsidianBackground.copy(alpha = 0f)
                            ),
                            center = Offset(size.width * 0.1f, size.height * 0.6f),
                            radius = size.maxDimension * 0.5f
                        )
                    )
                }
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideLayout = maxWidth >= 720.dp

                if (isWideLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1080.dp)
                            .align(Alignment.TopCenter)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HeroProtectionCard(
                                pairedCode = uiState.pairedCode,
                                runtimeState = uiState.runtimeState
                            )
                            FeedbackStatusBanner(banner = uiState.feedbackBanner)
                            PairingCard(
                                codeInput = uiState.codeInput,
                                isVerifying = uiState.isVerifying,
                                onCodeChanged = onCodeChanged,
                                onVerifyAndStart = onVerifyAndStart
                            )
                            EmergencySosCard(
                                isSendingSos = uiState.isSendingSos,
                                onSendSos = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendSos()
                                }
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LiveTelemetrySection(runtimeState = uiState.runtimeState)
                            QuickSetupSection(
                                uiState = uiState,
                                onDisableBatteryOpt = onDisableBatteryOpt,
                                onOpenAccessibility = onOpenAccessibility,
                                onOpenNotificationListener = onOpenNotificationListener
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp)
                            .align(Alignment.TopCenter)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeroProtectionCard(
                            pairedCode = uiState.pairedCode,
                            runtimeState = uiState.runtimeState
                        )

                        FeedbackStatusBanner(banner = uiState.feedbackBanner)

                        PairingCard(
                            codeInput = uiState.codeInput,
                            isVerifying = uiState.isVerifying,
                            onCodeChanged = onCodeChanged,
                            onVerifyAndStart = onVerifyAndStart
                        )

                        LiveTelemetrySection(runtimeState = uiState.runtimeState)

                        QuickSetupSection(
                            uiState = uiState,
                            onDisableBatteryOpt = onDisableBatteryOpt,
                            onOpenAccessibility = onOpenAccessibility,
                            onOpenNotificationListener = onOpenNotificationListener
                        )

                        EmergencySosCard(
                            isSendingSos = uiState.isSendingSos,
                            onSendSos = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSendSos()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingCard(
    codeInput: String,
    isVerifying: Boolean,
    onCodeChanged: (String) -> Unit,
    onVerifyAndStart: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = GuardCyan,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.pairing_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(R.string.pairing_section_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PairingCodeSlotsIndicator(code = codeInput)

            OutlinedTextField(
                value = codeInput,
                onValueChange = onCodeChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pairing_code_input"),
                label = {
                    Text(text = stringResource(R.string.pairing_code_label))
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.pairing_code_placeholder),
                        fontFamily = JetBrainsMonoFontFamily
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        onVerifyAndStart()
                    }
                ),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GuardCyan,
                    unfocusedBorderColor = ObsidianOutline,
                    focusedContainerColor = ObsidianSurfaceHigh,
                    unfocusedContainerColor = ObsidianSurfaceHigh,
                    cursorColor = GuardCyan,
                    focusedLabelColor = GuardCyan
                ),
                supportingText = {
                    Text(
                        text = "${codeInput.length}/6 digits",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (codeInput.length == 6) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Button(
                onClick = {
                    keyboardController?.hide()
                    onVerifyAndStart()
                },
                enabled = !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("verify_start_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuardCyan,
                    contentColor = OnGuardCyan,
                    disabledContainerColor = GuardCyan.copy(alpha = 0.45f),
                    disabledContentColor = OnGuardCyan
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnGuardCyan,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.btn_verifying),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.btn_verify_and_start),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSetupSection(
    uiState: GuardUiState,
    onDisableBatteryOpt: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenNotificationListener: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.quick_setup_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        QuickSetupActionCard(
            title = stringResource(R.string.btn_disable_battery_opt),
            subtitle = stringResource(R.string.btn_disable_battery_opt_desc),
            icon = Icons.Rounded.BatteryAlert,
            isEnabled = uiState.setupStatus.batteryOptimizationDisabled,
            testTag = "disable_battery_opt_button",
            onClick = onDisableBatteryOpt
        )

        QuickSetupActionCard(
            title = stringResource(R.string.btn_open_accessibility),
            subtitle = stringResource(R.string.btn_open_accessibility_desc),
            icon = Icons.Rounded.AccessibilityNew,
            isEnabled = uiState.setupStatus.accessibilityEnabled,
            testTag = "open_accessibility_button",
            onClick = onOpenAccessibility
        )

        QuickSetupActionCard(
            title = stringResource(R.string.btn_open_notification_listener),
            subtitle = stringResource(R.string.btn_open_notification_listener_desc),
            icon = Icons.Rounded.NotificationsActive,
            isEnabled = uiState.setupStatus.notificationListenerEnabled,
            testTag = "open_notification_listener_button",
            onClick = onOpenNotificationListener
        )
    }
}

@Composable
private fun EmergencySosCard(
    isSendingSos: Boolean,
    onSendSos: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SosCrimsonContainer.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, SosCrimson.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sos,
                    contentDescription = stringResource(R.string.cd_sos_icon),
                    tint = SosCrimson,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.emergency_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.btn_send_sos_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onSendSos,
                enabled = !isSendingSos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("sos_alert_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SosCrimson,
                    contentColor = OnSosCrimson,
                    disabledContainerColor = SosCrimsonDeep.copy(alpha = 0.6f),
                    disabledContentColor = OnSosCrimson
                )
            ) {
                if (isSendingSos) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnSosCrimson,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sending SOS Alert…",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Sos,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.btn_send_sos),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
