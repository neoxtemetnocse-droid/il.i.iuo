package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.work.*
import com.example.data.PreferencesManager
import com.example.network.SdmxApiService
import com.example.ui.MainScreen
import com.example.ui.SetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.worker.SdmxWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = PreferencesManager(this)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var isSetupConfigured by remember { mutableStateOf(prefs.userSdmx.isNotBlank() && prefs.passSdmx.isNotBlank()) }

                    if (!isSetupConfigured) {
                        SetupScreen(prefs = prefs) {
                            isSetupConfigured = true
                            scheduleWork(prefs.workIntervalHours)
                        }
                    } else {
                        MainScreen(
                            prefs = prefs,
                            onRunNow = {
                                val req = OneTimeWorkRequestBuilder<SdmxWorker>().build()
                                WorkManager.getInstance(applicationContext).enqueue(req)
                            },
                            onAddUser = { user, pass ->
                                addUserLogic(user, pass)
                            },
                            onIntervalChanged = { interval ->
                                scheduleWork(interval)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun scheduleWork(intervalHours: Int) {
        val workManager = WorkManager.getInstance(applicationContext)
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

    private fun addUserLogic(newUser: String, newPass: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val api = SdmxApiService()
            val cookie = api.login(prefs.userSdmx, prefs.passSdmx)
            if (cookie == null) {
                prefs.appendLog("❌ [AddUser] Login fallido al intentar agregar.")
                return@launch
            }

            val expCal = Calendar.getInstance()
            expCal.add(Calendar.MONTH, 1)
            val expDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val expDateStr = expDateFormat.format(expCal.time)
            val futureIsoStr = SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'", Locale.US).format(expCal.time)

            val created = api.createLineSdmx(newUser, newPass, expDateStr, cookie)
            if (created) {
                // Get the new ID using the getTableNewIds but spoofing vigentes
                val mockUser = com.example.data.SdmxUser(newUser, newPass, "", "")
                val mapIds = api.getTableNewIds(cookie, listOf(mockUser))
                val newId = mapIds[newUser]

                if (newId != null) {
                    val ins = api.insertRowSheets(newUser, newId, newPass, futureIsoStr)
                    if (ins) {
                        prefs.appendLog("✅ Usuario creado y guardado: $newUser | id: $newId | vence: $futureIsoStr")
                    } else {
                        prefs.appendLog("⚠️ Generado $newUser pero falló al guardar en Sheets.")
                    }
                } else {
                    prefs.appendLog("⚠️ Usuario creado $newUser pero no se pudo obtener su ID.")
                }
            } else {
                prefs.appendLog("❌ Falla al crear usuario $newUser en SDMX.")
            }
        }
    }
}
