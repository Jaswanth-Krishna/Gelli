package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.service.ProtectionRuntimeState
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionAmberContainer
import com.example.ui.theme.GuardCyan
import com.example.ui.theme.GuardCyanContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.ShieldEmeraldContainer
import com.example.ui.theme.SosCrimson
import com.example.ui.theme.SosCrimsonContainer
import java.util.Locale

@Composable
fun HeroProtectionCard(
    pairedCode: String?,
    runtimeState: ProtectionRuntimeState,
    modifier: Modifier = Modifier
) {
    val isProtected = runtimeState.isServiceRunning
    val statusColor = when {
        runtimeState.isAlarmRinging -> SosCrimson
        isProtected -> ShieldEmerald
        pairedCode != null -> GuardCyan
        else -> CautionAmber
    }
    val statusLabel = when {
        runtimeState.isAlarmRinging -> "ALARM ACTIVE"
        isProtected -> stringResource(R.string.status_protected)
        pairedCode != null -> stringResource(R.string.status_paired_standby)
        else -> stringResource(R.string.status_unpaired)
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 168.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_shield_hero_1795131847824),
                contentDescription = stringResource(R.string.cd_shield_hero),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.55f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ObsidianBackground.copy(alpha = 0.96f),
                                ObsidianBackground.copy(alpha = 0.78f),
                                ObsidianBackground.copy(alpha = 0.25f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isProtected || runtimeState.isAlarmRinging) {
                                            statusColor.copy(alpha = pulseAlpha)
                                        } else {
                                            statusColor
                                        }
                                    )
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!pairedCode.isNullOrBlank()) {
                        Surface(
                            color = GuardCyanContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, GuardCyan.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "ID $pairedCode",
                                style = MaterialTheme.typography.labelMedium,
                                color = GuardCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (runtimeState.isAlarmRinging) {
                    Surface(
                        color = SosCrimsonContainer,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, SosCrimson)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Alarm ringing",
                                tint = SosCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.service_notification_alarm_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackStatusBanner(
    banner: FeedbackBanner?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (banner != null) {
            val (containerColor, accentColor, icon) = when (banner.tone) {
                BannerTone.SUCCESS -> Triple(
                    ShieldEmeraldContainer,
                    ShieldEmerald,
                    Icons.Rounded.CheckCircle
                )
                BannerTone.ERROR -> Triple(
                    SosCrimsonContainer,
                    SosCrimson,
                    Icons.Rounded.ErrorOutline
                )
                BannerTone.INFO -> Triple(
                    GuardCyanContainer,
                    GuardCyan,
                    Icons.Rounded.Info
                )
            }

            Surface(
                color = containerColor,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = banner.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PairingCodeSlotsIndicator(
    code: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until 6) {
            val digit = code.getOrNull(i)?.toString() ?: ""
            val isFilled = digit.isNotEmpty()
            val isNext = i == code.length

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isFilled) GuardCyanContainer.copy(alpha = 0.65f)
                        else ObsidianSurfaceHigh
                    )
                    .border(
                        width = if (isNext) 1.5.dp else 1.dp,
                        color = when {
                            isFilled -> GuardCyan
                            isNext -> GuardCyan.copy(alpha = 0.65f)
                            else -> ObsidianOutline
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = if (isFilled) digit else "•",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isFilled) GuardCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
fun LiveTelemetrySection(
    runtimeState: ProtectionRuntimeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.telemetry_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (runtimeState.isServiceRunning) "10s GPS · 30s Route" else "Standby",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (runtimeState.isServiceRunning) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val coordsLabel = if (runtimeState.latitude != null && runtimeState.longitude != null) {
                String.format(
                    Locale.US,
                    "%.4f, %.4f",
                    runtimeState.latitude,
                    runtimeState.longitude
                )
            } else {
                "Waiting for GPS"
            }

            val batteryLabel = if (runtimeState.batteryPercent in 0..100) {
                "${runtimeState.batteryPercent}%"
            } else {
                "—"
            }

            val safeZoneLabel = when {
                !runtimeState.hasGeofence -> "No Fence Set"
                runtimeState.insideSafeZone -> "Inside (${runtimeState.geofenceRadiusMeters.toInt()}m)"
                else -> "Outside Zone!"
            }

            val safeZoneColor = when {
                !runtimeState.hasGeofence -> MaterialTheme.colorScheme.onSurfaceVariant
                runtimeState.insideSafeZone -> ShieldEmerald
                else -> SosCrimson
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryTile(
                    icon = Icons.Rounded.GpsFixed,
                    label = "Location",
                    value = coordsLabel,
                    accent = GuardCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryTile(
                    icon = Icons.Rounded.BatteryChargingFull,
                    label = "Battery",
                    value = batteryLabel,
                    accent = ShieldEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryTile(
                    icon = if (runtimeState.insideSafeZone) Icons.Rounded.Security else Icons.Rounded.Warning,
                    label = "Safe Zone",
                    value = safeZoneLabel,
                    accent = safeZoneColor,
                    modifier = Modifier.weight(1f)
                )
                TelemetryTile(
                    icon = Icons.Rounded.Block,
                    label = "Blocked Apps",
                    value = "${runtimeState.blockedAppsCount} active",
                    accent = CautionAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ObsidianSurfaceHigh,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickSetupActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeBg = if (isEnabled) ShieldEmeraldContainer else CautionAmberContainer
    val badgeTextColor = if (isEnabled) ShieldEmerald else CautionAmber
    val badgeLabel = if (isEnabled) {
        stringResource(R.string.status_enabled)
    } else {
        stringResource(R.string.status_required)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(
            1.dp,
            if (isEnabled) ShieldEmerald.copy(alpha = 0.35f) else ObsidianOutline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isEnabled) ShieldEmeraldContainer else GuardCyanContainer
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isEnabled) ShieldEmerald else GuardCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Open settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
