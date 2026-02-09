package com.saarthi.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.saarthi.app.R
import com.saarthi.app.network.ApiClient
import kotlinx.coroutines.*

class HackerDashboard : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var logBox: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hacker)

        status = findViewById(R.id.statusText)
        logBox = findViewById(R.id.logBox)

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            testConnection()
        }

        findViewById<Button>(R.id.btnScan).setOnClickListener {
            log("Scanning...")
        }

        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            log("Uploading data...")
        }
    }

    private fun testConnection() {

        status.text = "Connecting..."

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val response = ApiClient.client.baseUrl()

                withContext(Dispatchers.Main) {
                    status.text = "Connected ✅"
                    log("Server Connected")
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    status.text = "Failed ❌"
                    log("Connection Failed")
                }
            }
        }
    }

    private fun log(msg: String) {

        logBox.append("➤ $msg\n")

    }
}
