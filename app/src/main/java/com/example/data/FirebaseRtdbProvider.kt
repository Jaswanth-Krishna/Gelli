package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseRtdbProvider {
    const val DATABASE_URL =
        "https://i-w-flashget-parental-control-default-rtdb.firebaseio.com/"

    @Synchronized
    fun getDatabase(context: Context): FirebaseDatabase {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:000000000000:android:flashgetguard001")
                .setApiKey("AIzaSyFlashGetGuardPlaceholderKey000000")
                .setProjectId("i-w-flashget-parental-control")
                .setDatabaseUrl(DATABASE_URL)
                .build()
            FirebaseApp.initializeApp(appContext, options)
        }
        return FirebaseDatabase.getInstance(DATABASE_URL)
    }

    fun codeRef(context: Context, code: String): DatabaseReference =
        getDatabase(context).reference
            .child("parental_control")
            .child("codes")
            .child(code)

    fun sessionRef(context: Context, code: String): DatabaseReference =
        getDatabase(context).reference
            .child("parental_control")
            .child("sessions")
            .child(code)

    fun telemetryRef(context: Context, code: String): DatabaseReference =
        sessionRef(context, code).child("telemetry")

    fun historyRef(context: Context, code: String): DatabaseReference =
        sessionRef(context, code).child("history")

    fun geofenceRef(context: Context, code: String): DatabaseReference =
        sessionRef(context, code).child("geofence")

    fun commandsActionRef(context: Context, code: String): DatabaseReference =
        sessionRef(context, code).child("commands").child("action")

    fun alertsRef(context: Context, code: String): DatabaseReference =
        sessionRef(context, code).child("alerts")

    fun blockedAppsRef(context: Context, code: String): DatabaseReference =
        sessionRef(context, code).child("blocked_apps")
}
