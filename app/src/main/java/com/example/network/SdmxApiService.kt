package com.example.network

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.SdmxUser
import java.io.IOException
import java.util.concurrent.TimeUnit

class SdmxApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun login(user: String, pass: String): String? {
        val formBody = "referrer=&username=$user&password=$pass&login=".toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://sdmx.vip/resellers/login")
            .post(formBody)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146.0.0.0 Safari/537.36")
            .addHeader("Origin", "https://sdmx.vip")
            .addHeader("Referer", "https://sdmx.vip/resellers/login")
            .addHeader("Connection", "keep-alive")
            .build()
        try {
            val response = client.newCall(request).execute()
            val cookies = response.headers("Set-Cookie")
            if (cookies.isNotEmpty()) {
                val cookieStr = cookies.joinToString("; ") { it.substringBefore(";") }
                return cookieStr
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getSheetsData(): List<SdmxUser> {
        val request = Request.Builder()
            .url("https://script.google.com/macros/s/AKfycbxiBjtQmyOubnbyJQfJrT4Vs0DhJ94vSnPgfkCwUirMfcD3GRqGflKC--e1NXkHCl-V/exec?hoja=Permanentes")
            .get()
            .build()
        val users = mutableListOf<SdmxUser>()
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (body != null) {
                val json = JSONObject(body)
                if (json.optString("status") == "ok") {
                    val datos = json.optJSONArray("datos")
                    if (datos != null) {
                        for (i in 0 until datos.length()) {
                            val userObj = datos.optJSONObject(i)
                            if (userObj != null) {
                                users.add(
                                    SdmxUser(
                                        usuario = userObj.optString("usuario"),
                                        password = userObj.optString("password"),
                                        vencimiento = userObj.optString("vencimiento"),
                                        id = userObj.optString("id")
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return users
    }

    fun deleteLineSdmx(id: String, cookie: String): Boolean {
        val request = Request.Builder()
            .url("https://sdmx.vip/resellers/api?action=line&sub=delete&user_id=$id")
            .get()
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("Cookie", cookie)
            .addHeader("Referer", "https://sdmx.vip/resellers/lines?order=0&dir=desc")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146.0.0.0 Safari/537.36")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()
        try {
            val response = client.newCall(request).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun createLineSdmx(usuario: String, pass: String, expDate: String, cookie: String): Boolean {
        val bodyStr = "action=line&trial=1&bouquets_selected=&username=$usuario&password=$pass&package=150&package_cost=0&package_duration=24 hours&max_connections=2&exp_date=$expDate&contact=&reseller_notes=&isp_clear=&bouquets_selected[]=19&bouquets_selected[]=24&bouquets_selected[]=21&bouquets_selected[]=8&bouquets_selected[]=23&bouquets_selected[]=96"
        val body = bodyStr.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url("https://sdmx.vip/resellers/post.php?action=line")
            .post(body)
            .addHeader("Cookie", cookie)
            .addHeader("Origin", "https://sdmx.vip")
            .addHeader("Referer", "https://sdmx.vip/resellers/line?trial=1")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()
        try {
            val response = client.newCall(request).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun getTableNewIds(cookie: String, vigentes: List<SdmxUser>): Map<String, String> {
        val request = Request.Builder()
            .url("https://sdmx.vip/resellers/table?draw=1&id=lines&filter=&reseller=&start=0&length=-1&order[0][column]=0&order[0][dir]=desc&search[value]=&search[regex]=false")
            .get()
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("Cookie", cookie)
            .addHeader("Referer", "https://sdmx.vip/resellers/lines?order=0&dir=desc")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()
            
        val newUserIdMap = mutableMapOf<String, String>()
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (body != null) {
                if (body.trim().startsWith("{")) {
                    val json = JSONObject(body)
                    val dataArray = json.optJSONArray("data")
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val row = dataArray.optJSONArray(i)
                            if (row != null) {
                                val col0 = row.optString(0, "")
                                val col1 = row.optString(1, "")

                                val newId = Regex(""">(\d+)<""").find(col0)?.groupValues?.get(1)
                                val username = Regex(""">([^<]+)</a>""").find(col1)?.groupValues?.get(1)

                                if (newId != null && username != null) {
                                    val matchedUser = vigentes.find { it.usuario == username }
                                    if (matchedUser != null) {
                                        newUserIdMap[matchedUser.usuario] = newId
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newUserIdMap
    }

    fun deleteRowSheets(usuario: String): Boolean {
        val jsonPayload = JSONObject().apply {
            put("hoja", "Permanentes")
            put("action", "delete")
            put("user", usuario)
        }.toString()
        val body = jsonPayload.toRequestBody("application/json".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url("https://script.google.com/macros/s/AKfycbxiBjtQmyOubnbyJQfJrT4Vs0DhJ94vSnPgfkCwUirMfcD3GRqGflKC--e1NXkHCl-V/exec")
            .post(body)
            .build()
        try {
            val response = client.newCall(request).execute()
            android.util.Log.d("Sheets Delete", response.body?.string() ?: "")
            return response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun insertRowSheets(usuario: String, id: String, password: String, vencimiento: String): Boolean {
        val userObj = JSONObject().apply {
            put("id", id)
            put("usuario", usuario)
            put("password", password)
            put("vencimiento", vencimiento)
        }
        val datosArray = JSONArray().apply { put(userObj) }
        val jsonPayload = JSONObject().apply {
            put("hoja", "Permanentes")
            put("datos", datosArray)
        }.toString()
        
        val body = jsonPayload.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://script.google.com/macros/s/AKfycbxiBjtQmyOubnbyJQfJrT4Vs0DhJ94vSnPgfkCwUirMfcD3GRqGflKC--e1NXkHCl-V/exec")
            .post(body)
            .build()
        try {
            val response = client.newCall(request).execute()
            android.util.Log.d("Sheets Insert", response.body?.string() ?: "")
            return response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
