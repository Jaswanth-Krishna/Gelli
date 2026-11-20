package com.example.data

import android.content.Context
import android.content.SharedPreferences

object GuardPrefs {
    const val PREFS_NAME = "flashget_prefs"
    const val KEY_PAIRING_CODE = "pairing_code"

    fun getPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPairingCode(context: Context): String? {
        val code = getPrefs(context).getString(KEY_PAIRING_CODE, null)?.trim()
        return code?.takeIf { it.length == 6 && it.all(Char::isDigit) }
    }

    fun savePairingCode(context: Context, code: String) {
        getPrefs(context).edit().putString(KEY_PAIRING_CODE, code.trim()).apply()
    }

    fun clearPairingCode(context: Context) {
        getPrefs(context).edit().remove(KEY_PAIRING_CODE).apply()
    }
}
