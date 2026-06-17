package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.PreferencesManager
import com.example.data.SdmxUser
import com.example.network.SdmxApiService
import com.example.notifications.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*

class SdmxWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val prefs = PreferencesManager(appContext)
    private val api = SdmxApiService()
    private val notifier = NotificationHelper(appContext)

    override suspend fun doWork(): Result {
        val user = prefs.userSdmx
        val pass = prefs.passSdmx
        if (user.isEmpty() || pass.isEmpty()) {
            notifier.showNotification("Error", "Credenciales no configuradas.")
            return Result.failure()
        }

        prefs.appendLog("Iniciando ciclo de renovación...")

        val cookie = api.login(user, pass)
        if (cookie == null) {
            prefs.appendLog("❌ Login fallido.")
            notifier.showNotification("SDMX Error", "Login fallido.")
            return Result.failure()
        }
        prefs.loginCookie = cookie

        val allUsers = api.getSheetsData()
        if (allUsers.isEmpty()) {
            prefs.appendLog("❌ Error: No se leyeron usuarios de Sheets.")
            notifier.showNotification("SDMX Error", "Error leyendo Sheets.")
            return Result.failure()
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val vigentes = mutableListOf<SdmxUser>()
        val noVigentes = mutableListOf<SdmxUser>()

        allUsers.forEach { u ->
            val fechaLimpia = u.vencimiento.trim().substringBefore("T")
            try {
                val fechaVec = sdf.parse(fechaLimpia)
                if (fechaVec != null && !fechaVec.before(todayCal)) {
                    vigentes.add(u)
                } else {
                    noVigentes.add(u)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        prefs.appendLog("📋 Total leídos: ${allUsers.size} | Vigentes: ${vigentes.size} | No vigentes: ${noVigentes.size}")

        val expCal = Calendar.getInstance()
        expCal.add(Calendar.MONTH, 1)
        val expDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val expDateStr = expDateFormat.format(expCal.time)
        val futureIsoStr = SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'", Locale.US).format(expCal.time)

        vigentes.forEach { vUser ->
            api.deleteLineSdmx(vUser.id, cookie)
            api.createLineSdmx(vUser.usuario, vUser.password, expDateStr, cookie)
        }

        val idsMap = api.getTableNewIds(cookie, vigentes)

        var renovados = 0
        vigentes.forEach { vUser ->
            val newId = idsMap[vUser.usuario]
            if (newId != null) {
                api.deleteRowSheets(vUser.usuario)
                api.insertRowSheets(vUser.usuario, newId, vUser.password, futureIsoStr)
                renovados++
            } else {
                prefs.appendLog("⚠️ No se encontró nuevo ID para ${vUser.usuario}")
            }
        }

        val nextHours = prefs.workIntervalHours
        prefs.appendLog("🎉 Ciclo completado. Procesados: $renovados usuarios vigentes.\nPróxima ejecución en: $nextHours horas.")
        notifier.showNotification("SDMX Auto-Renew", "Ciclo completado. $renovados usuarios renovados. Próxima ejecución en $nextHours horas.")

        return Result.success()
    }
}
