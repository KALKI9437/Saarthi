package com.saarthi.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.saarthi.app.R
import com.saarthi.app.network.ApiClient
import com.saarthi.app.permissions.StoragePermission
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HackerDashboard : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var logBox: TextView

    // Coroutine scope (safe lifecycle)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hacker)

        // ✅ Ask for full storage permission
        StoragePermission.request(this)

        // UI references
        status = findViewById(R.id.statusText)
        logBox = findViewById(R.id.logBox)

        // Buttons
        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            testConnection()
        }

        findViewById<Button>(R.id.btnScan).setOnClickListener {
            log("System scan started...")
        }

        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            log("Uploading data to server...")
        }

        log("Hacker Dashboard Loaded")
    }

    // ✅ Real server connection test (FIXED)
    private fun testConnection() {

        status.text = "Connecting..."
        log("Trying to connect to server...")

        scope.launch {

            try {

                val baseUrl = ApiClient.client.baseUrl().toString()

                val client = OkHttpClient()

                val request = Request.Builder()
                    .url(baseUrl)
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {

                    if (response.isSuccessful) {

                        status.text = "Connected ✅"
                        log("Server connected successfully")

                    } else {

                        status.text = "Server Error ❌"
                        log("Server responded but not OK")   // ✅ FIXED
                    }
                }

                response.close()

            } catch (e: IOException) {

                withContext(Dispatchers.Main) {
                    status.text = "No Internet ❌"
                    log("Network error: No connection")
                }

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    status.text = "Failed ❌"
                    log("Error: ${e.message}")
                }
            }
        }
    }

    // ✅ Log system
    private fun log(msg: String) {

        runOnUiThread {
            logBox.append("➤ $msg\n")
        }
    }

    // ✅ Prevent memory leak
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
