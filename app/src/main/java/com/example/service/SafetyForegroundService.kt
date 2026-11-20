package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FirebaseRtdbProvider
import com.example.data.GuardPrefs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SafetyForegroundService : Service() {

    private data class GeofenceZone(
        val lat: Double,
        val lng: Double,
        val radiusMeters: Double
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var activeCode: String? = null
    private var locationCallback: LocationCallback? = null
    private var telemetryLoopJob: Job? = null
    private var historyLoopJob: Job? = null

    private var geofenceRef: DatabaseReference? = null
    private var geofenceListener: ValueEventListener? = null

    private var commandActionRef: DatabaseReference? = null
    private var commandActionListener: ValueEventListener? = null

    @Volatile
    private var latestLocation: Location? = null

    @Volatile
    private var activeGeofence: GeofenceZone? = null

    @Volatile
    private var wasOutsideSafeZone: Boolean = false

    @Volatile
    private var alarmPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getStringExtra(EXTRA_PAIRING_CODE)
            ?: GuardPrefs.getPairingCode(this)

        if (code.isNullOrBlank()) {
            Log.w(TAG, "No pairing code found; stopping SafetyForegroundService.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!promoteToForeground(code, isAlarmRinging = alarmPlayer?.isPlaying == true)) {
            Log.e(TAG, "Unable to enter foreground due to missing required runtime permissions.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (activeCode != code) {
            detachSessionListeners()
            activeCode = code
            attachSessionListeners(code)
            startLocationUpdates()
            startTelemetryAndHistoryLoops(code)
        } else if (telemetryLoopJob?.isActive != true) {
            startLocationUpdates()
            startTelemetryAndHistoryLoops(code)
        }

        ServiceStateBus.update {
            it.copy(
                isServiceRunning = true,
                activeCode = code,
                batteryPercent = readBatteryPercent()
            )
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlarm()
        stopLocationUpdates()
        detachSessionListeners()
        telemetryLoopJob?.cancel()
        historyLoopJob?.cancel()

        activeCode?.let { code ->
            runCatching {
                FirebaseRtdbProvider.telemetryRef(this, code)
                    .child("status")
                    .setValue("OFFLINE")
            }
        }

        ServiceStateBus.update {
            it.copy(
                isServiceRunning = false,
                isAlarmRinging = false
            )
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    private fun promoteToForeground(code: String, isAlarmRinging: Boolean): Boolean {
        val notification = buildNotification(code, isAlarmRinging)
        val serviceTypes = computeEligibleForegroundTypes()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (serviceTypes != 0) {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceTypes)
                } else {
                    // On Android 14+, starting a typed FGS without at least one matching granted permission
                    // throws SecurityException; guard cleanly.
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException starting foreground service: ${se.message}", se)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}", e)
            false
        }
    }

    private fun computeEligibleForegroundTypes(): Int {
        var types = 0
        if (hasLocationPermission()) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (hasPermission(Manifest.permission.CAMERA)) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            }
            if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
        }
        return types
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_safety_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_safety_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(code: String, isAlarmRinging: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (isAlarmRinging) {
            getString(R.string.service_notification_alarm_text)
        } else {
            "${getString(R.string.service_notification_text)} (Code $code)"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(
                if (isAlarmRinging) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_LOW
            )
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(isAlarmRinging: Boolean) {
        val code = activeCode ?: return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(code, isAlarmRinging))
    }

    private fun startLocationUpdates() {
        stopLocationUpdates()
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted; skipping FusedLocation updates.")
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TELEMETRY_INTERVAL_MS)
            .setMinUpdateIntervalMillis(TELEMETRY_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                latestLocation = loc
                activeCode?.let { code ->
                    evaluateGeofenceAndAlert(code, loc)
                }
            }
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && latestLocation == null) {
                    latestLocation = lastLoc
                }
            }
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
            locationCallback = callback
        } catch (se: SecurityException) {
            Log.e(TAG, "Location SecurityException: ${se.message}", se)
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            runCatching { fusedLocationClient.removeLocationUpdates(callback) }
        }
        locationCallback = null
    }

    private fun startTelemetryAndHistoryLoops(code: String) {
        telemetryLoopJob?.cancel()
        historyLoopJob?.cancel()

        // Configure onDisconnect status fallback
        runCatching {
            FirebaseRtdbProvider.telemetryRef(this, code)
                .child("status")
                .onDisconnect()
                .setValue("OFFLINE")
        }

        telemetryLoopJob = serviceScope.launch {
            while (isActive) {
                pushTelemetrySnapshot(code)
                delay(TELEMETRY_INTERVAL_MS)
            }
        }

        historyLoopJob = serviceScope.launch {
            while (isActive) {
                delay(HISTORY_INTERVAL_MS)
                pushHistoryPoint(code)
            }
        }
    }

    private fun pushTelemetrySnapshot(code: String) {
        val loc = latestLocation
        val lat = loc?.latitude ?: 0.0
        val lng = loc?.longitude ?: 0.0
        val speed = loc?.speed ?: 0f
        val battery = readBatteryPercent()
        val insideSafeZone = if (loc != null) {
            evaluateGeofenceAndAlert(code, loc)
        } else {
            true
        }

        val payload = mapOf<String, Any>(
            "lat" to lat,
            "lng" to lng,
            "speed" to speed,
            "battery" to battery,
            "insideSafeZone" to insideSafeZone,
            "status" to "ONLINE",
            "updatedAt" to ServerValue.TIMESTAMP
        )

        FirebaseRtdbProvider.telemetryRef(this, code).setValue(payload)

        ServiceStateBus.update {
            it.copy(
                isServiceRunning = true,
                activeCode = code,
                latitude = loc?.latitude,
                longitude = loc?.longitude,
                speedMetersPerSec = speed,
                batteryPercent = battery,
                insideSafeZone = insideSafeZone,
                lastSyncTimestampMs = System.currentTimeMillis()
            )
        }
    }

    private fun pushHistoryPoint(code: String) {
        val loc = latestLocation ?: return
        val historyPayload = mapOf<String, Any>(
            "lat" to loc.latitude,
            "lng" to loc.longitude,
            "time" to ServerValue.TIMESTAMP
        )
        FirebaseRtdbProvider.historyRef(this, code).push().setValue(historyPayload)
    }

    private fun attachSessionListeners(code: String) {
        // 1. Geofence listener
        val gRef = FirebaseRtdbProvider.geofenceRef(this, code)
        val gListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    activeGeofence = null
                    wasOutsideSafeZone = false
                    ServiceStateBus.update {
                        it.copy(hasGeofence = false, geofenceRadiusMeters = 0.0, insideSafeZone = true)
                    }
                    return
                }
                val lat = snapshot.child("lat").getValue(Double::class.java)
                val lng = snapshot.child("lng").getValue(Double::class.java)
                val radius = snapshot.child("radius").getValue(Double::class.java)

                if (lat != null && lng != null && radius != null && radius > 0.0) {
                    activeGeofence = GeofenceZone(lat, lng, radius)
                    ServiceStateBus.update {
                        it.copy(hasGeofence = true, geofenceRadiusMeters = radius)
                    }
                    latestLocation?.let { loc -> evaluateGeofenceAndAlert(code, loc) }
                } else {
                    activeGeofence = null
                    ServiceStateBus.update {
                        it.copy(hasGeofence = false, geofenceRadiusMeters = 0.0)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Geofence listener cancelled: ${error.message}")
            }
        }
        gRef.addValueEventListener(gListener)
        geofenceRef = gRef
        geofenceListener = gListener

        // 2. Remote command action listener
        val cRef = FirebaseRtdbProvider.commandsActionRef(this, code)
        val cListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val action = snapshot.getValue(String::class.java)?.trim()?.uppercase() ?: return
                when (action) {
                    COMMAND_RING_ALARM -> startAlarm()
                    COMMAND_STOP_ALARM -> {
                        stopAlarm()
                        cRef.setValue(COMMAND_IDLE)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Commands listener cancelled: ${error.message}")
            }
        }
        cRef.addValueEventListener(cListener)
        commandActionRef = cRef
        commandActionListener = cListener
    }

    private fun detachSessionListeners() {
        geofenceListener?.let { listener -> geofenceRef?.removeEventListener(listener) }
        geofenceListener = null
        geofenceRef = null

        commandActionListener?.let { listener -> commandActionRef?.removeEventListener(listener) }
        commandActionListener = null
        commandActionRef = null
    }

    private fun evaluateGeofenceAndAlert(code: String, location: Location): Boolean {
        val zone = activeGeofence ?: return true
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            zone.lat,
            zone.lng,
            results
        )
        val distanceMeters = results[0]
        val inside = distanceMeters <= zone.radiusMeters

        if (!inside && !wasOutsideSafeZone) {
            wasOutsideSafeZone = true
            val alertPayload = mapOf<String, Any>(
                "type" to "GEOFENCE_EXIT",
                "message" to "ALERT: Device exited safe zone (${distanceMeters.toInt()}m away, limit ${zone.radiusMeters.toInt()}m)",
                "time" to ServerValue.TIMESTAMP
            )
            FirebaseRtdbProvider.alertsRef(this, code).push().setValue(alertPayload)
        } else if (inside) {
            wasOutsideSafeZone = false
        }

        ServiceStateBus.update { it.copy(insideSafeZone = inside) }
        return inside
    }

    @Synchronized
    private fun startAlarm() {
        if (alarmPlayer?.isPlaying == true) return
        try {
            val audioManager = getSystemService(AudioManager::class.java)
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                runCatching {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                }
            }

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            alarmPlayer = player
            ServiceStateBus.update { it.copy(isAlarmRinging = true) }
            updateNotification(isAlarmRinging = true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm playback: ${e.message}", e)
        }
    }

    @Synchronized
    private fun stopAlarm() {
        try {
            alarmPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping alarm player: ${e.message}")
        } finally {
            alarmPlayer = null
            ServiceStateBus.update { it.copy(isAlarmRinging = false) }
            updateNotification(isAlarmRinging = false)
        }
    }

    private fun readBatteryPercent(): Int {
        val batteryManager = getSystemService(BatteryManager::class.java)
        val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (capacity != null && capacity in 0..100) capacity else -1
    }

    private fun hasLocationPermission(): Boolean =
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "SafetyForegroundService"
        private const val CHANNEL_ID = "flashget_safety_protection_channel"
        private const val NOTIFICATION_ID = 1001

        private const val TELEMETRY_INTERVAL_MS = 10_000L
        private const val HISTORY_INTERVAL_MS = 30_000L

        const val EXTRA_PAIRING_CODE = "extra_pairing_code"
        private const val COMMAND_RING_ALARM = "RING_ALARM"
        private const val COMMAND_STOP_ALARM = "STOP_ALARM"
        private const val COMMAND_IDLE = "IDLE"

        fun start(context: Context, pairingCode: String) {
            val intent = Intent(context, SafetyForegroundService::class.java).apply {
                putExtra(EXTRA_PAIRING_CODE, pairingCode)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SafetyForegroundService::class.java))
        }
    }
}
