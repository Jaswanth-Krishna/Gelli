package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.FirebaseRtdbProvider
import com.example.data.GuardPrefs
import com.google.firebase.database.ServerValue

class NotificationSyncService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName?.trim() ?: return
        if (shouldIgnorePackage(pkg)) return

        val notification = sbn.notification ?: return
        // Ignore ongoing background service status notifications to avoid noise
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        if (title.isEmpty() && text.isEmpty()) return

        val code = GuardPrefs.getPairingCode(this) ?: return

        val formattedMessage = "[$pkg] $title: $text"
        val alertPayload = mapOf<String, Any>(
            "type" to "NOTIFICATION",
            "message" to formattedMessage,
            "time" to ServerValue.TIMESTAMP
        )

        runCatching {
            FirebaseRtdbProvider.alertsRef(this, code).push().setValue(alertPayload)
            ServiceStateBus.update { it.copy(lastSyncedNotificationPkg = pkg) }
        }.onFailure { error ->
            Log.w(TAG, "Failed to sync notification from $pkg: ${error.message}")
        }
    }

    private fun shouldIgnorePackage(pkg: String): Boolean {
        if (pkg == applicationContext.packageName) return true
        if (pkg in IGNORED_SYSTEM_PACKAGES) return true
        if (pkg.startsWith("com.android.providers.")) return true
        return false
    }

    companion object {
        private const val TAG = "NotificationSyncService"
        private val IGNORED_SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.vending",
            "com.google.android.gms"
        )
    }
}
