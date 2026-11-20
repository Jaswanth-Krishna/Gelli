package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.data.FirebaseRtdbProvider
import com.example.data.GuardPrefs
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.Collections

class AppBlockerService : AccessibilityService() {

    private val blockedNormalizedKeys: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    private var blockedAppsRef: DatabaseReference? = null
    private var blockedAppsListener: ValueEventListener? = null
    private var currentCode: String? = null

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == GuardPrefs.KEY_PAIRING_CODE) {
                rebindBlockedAppsListener()
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        GuardPrefs.getPrefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
        rebindBlockedAppsListener()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val foregroundPkg = event.packageName?.toString()?.trim() ?: return
        if (foregroundPkg.isEmpty()) return

        // Never block ourselves or critical Android system UI
        if (foregroundPkg == applicationContext.packageName || foregroundPkg == SYSTEM_UI_PACKAGE) {
            return
        }

        val underscoredPkg = foregroundPkg.replace('.', '_')
        val isBlocked = synchronized(blockedNormalizedKeys) {
            blockedNormalizedKeys.contains(underscoredPkg) ||
                blockedNormalizedKeys.contains(foregroundPkg)
        }

        if (isBlocked) {
            Log.i(TAG, "Blocked app detected in foreground: $foregroundPkg -> Returning HOME")
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {
        // Required override; no blocking audio/haptic feedback to interrupt.
    }

    override fun onDestroy() {
        GuardPrefs.getPrefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        detachBlockedAppsListener()
        super.onDestroy()
    }

    private fun rebindBlockedAppsListener() {
        val code = GuardPrefs.getPairingCode(this)
        if (code == currentCode && blockedAppsListener != null) return

        detachBlockedAppsListener()
        currentCode = code

        if (code.isNullOrBlank()) {
            blockedNormalizedKeys.clear()
            ServiceStateBus.update { it.copy(blockedAppsCount = 0) }
            return
        }

        val ref = FirebaseRtdbProvider.blockedAppsRef(this, code)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updated = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val key = child.key?.trim() ?: continue
                    val isEnabled = child.getValue(Boolean::class.java) == true
                    if (isEnabled && key.isNotEmpty()) {
                        updated.add(key)
                        // Also add dot-converted form for matching safety
                        updated.add(key.replace('_', '.'))
                    }
                }
                synchronized(blockedNormalizedKeys) {
                    blockedNormalizedKeys.clear()
                    blockedNormalizedKeys.addAll(updated)
                }
                val rawCount = snapshot.children.count { it.getValue(Boolean::class.java) == true }
                ServiceStateBus.update { it.copy(blockedAppsCount = rawCount) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Blocked apps listener cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        blockedAppsRef = ref
        blockedAppsListener = listener
    }

    private fun detachBlockedAppsListener() {
        blockedAppsListener?.let { listener ->
            blockedAppsRef?.removeEventListener(listener)
        }
        blockedAppsListener = null
        blockedAppsRef = null
    }

    companion object {
        private const val TAG = "AppBlockerService"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
