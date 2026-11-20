package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProtectionRuntimeState(
    val isServiceRunning: Boolean = false,
    val activeCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedMetersPerSec: Float = 0f,
    val batteryPercent: Int = -1,
    val insideSafeZone: Boolean = true,
    val hasGeofence: Boolean = false,
    val geofenceRadiusMeters: Double = 0.0,
    val isAlarmRinging: Boolean = false,
    val blockedAppsCount: Int = 0,
    val lastSyncTimestampMs: Long = 0L,
    val lastSyncedNotificationPkg: String? = null
)

object ServiceStateBus {
    private val _state = MutableStateFlow(ProtectionRuntimeState())
    val state: StateFlow<ProtectionRuntimeState> = _state.asStateFlow()

    fun update(transform: (ProtectionRuntimeState) -> ProtectionRuntimeState) {
        _state.update(transform)
    }
}
