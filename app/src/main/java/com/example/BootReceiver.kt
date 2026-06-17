package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.example.data.PreferencesManager
import com.example.worker.SdmxWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PreferencesManager(context)
            if (prefs.userSdmx.isNotBlank()) {
                val intervalHours = prefs.workIntervalHours
                val workManager = WorkManager.getInstance(context)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .build()
                val workRequest = PeriodicWorkRequestBuilder<SdmxWorker>(
                    intervalHours.toLong(), TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    "SdmxAutoRenew",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            }
        }
    }
}
