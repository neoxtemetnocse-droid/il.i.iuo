package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sdmx_prefs", Context.MODE_PRIVATE)

    var userSdmx: String
        get() = prefs.getString("user_sdmx", "") ?: ""
        set(value) = prefs.edit().putString("user_sdmx", value).apply()

    var passSdmx: String
        get() = prefs.getString("pass_sdmx", "") ?: ""
        set(value) = prefs.edit().putString("pass_sdmx", value).apply()

    var workIntervalHours: Int
        get() = prefs.getInt("work_interval_hours", 24)
        set(value) = prefs.edit().putInt("work_interval_hours", value).apply()

    var loginCookie: String
        get() = prefs.getString("login_cookie", "") ?: ""
        set(value) = prefs.edit().putString("login_cookie", value).apply()

    var lastRunLogs: String
        get() = prefs.getString("last_run_logs", "Sin registros.") ?: "Sin registros."
        set(value) = prefs.edit().putString("last_run_logs", value).apply()

    fun appendLog(log: String) {
        val current = lastRunLogs
        val newLog = if (current == "Sin registros.") log else "$current\n$log"
        lastRunLogs = newLog
    }
}
