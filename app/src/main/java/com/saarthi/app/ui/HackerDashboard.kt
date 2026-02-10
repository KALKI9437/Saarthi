package com.saarthi.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.saarthi.app.R
import com.saarthi.app.permissions.StoragePermission
import com.saarthi.app.backup.*
import com.saarthi.app.data.PrefManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HackerDashboard : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var logBox: TextView
    private lateinit var progress: ProgressBar

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val PICK_FOLDER = 101
    private var selectedFolder: Uri? = null

    // 🔴 CHANGE THESE
    private val SERVER_URL = "http://YOUR_SERVER/upload"
    private val HEALTH_URL = "http://YOUR_SERVER/health"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hacker)

        // Permissions
        StoragePermission.request(this)

        status = findViewById(R.id.statusText)
        logBox = findViewById(R.id.logBox)
        progress = findViewById(R.id.progressBar)

        // Save server once (Phase-2 requirement)
        PrefManager.saveServer(this, SERVER_URL)

        // CONNECT
        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            testConnection()
        }

        // PICK FOLDER
        findViewById<Button>(R.id.btnScan).setOnClickListener {
            pickFolder()
        }

        // MANUAL BACKUP (Phase-1)
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            startBackup()
        }

        // AUTO BACKUP (Phase-2)
        findViewById<Button>(R.id.btnAuto).setOnClickListener {
            startAutoBackup()
        }

        log("Dashboard Loaded ✅")
    }

    // ==================================================
    // SERVER CONNECTION TEST
    // ==================================================

    private fun testConnection() {

        status.text = "Connecting..."
        log("Checking server...")

        scope.launch {

            try {

                val request = Request.Builder()
                    .url(HEALTH_URL)
                    .build()

                val response = OkHttpClient()
                    .newCall(request)
                    .execute()

                withContext(Dispatchers.Main) {

                    if (response.isSuccessful) {
                        status.text = "Connected ✅"
                        log("Server OK")
                    } else {
                        status.text = "Server Error ❌"
                        log("Health API failed")
                    }
                }

                response.close()

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    status.text = "Failed ❌"
                    log("Server not reachable")
                }
            }
        }
    }

    // ==================================================
    // PICK FOLDER
    // ==================================================

    private fun pickFolder() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )

        startActivityForResult(intent, PICK_FOLDER)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FOLDER && resultCode == Activity.RESULT_OK) {

            selectedFolder = data?.data

            if (selectedFolder != null) {

                contentResolver.takePersistableUriPermission(
                    selectedFolder!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // 🔥 STEP-5: Save folder for auto backup
                PrefManager.saveFolder(
                    this,
                    selectedFolder.toString()
                )

                log("Folder Selected & Saved ✅")
                status.text = "Folder Ready"
            }
        }
    }

    // ==================================================
    // MANUAL BACKUP (PHASE-1)
    // ==================================================

    private fun startBackup() {

        if (selectedFolder == null) {
            log("❗ Select folder first")
            status.text = "No Folder"
            return
        }

        status.text = "Backing up..."
        progress.progress = 0
        log("Backup started...")

        scope.launch {

            val files = FileScanner.scanFolder(
                this@HackerDashboard,
                selectedFolder!!
            )

            if (files.isEmpty()) {
                withContext(Dispatchers.Main) {
                    log("No files found")
                    status.text = "Nothing to backup"
                }
                return@launch
            }

            val total = files.size
            var success = 0

            for ((index, doc) in files.withIndex()) {

                val ok = RetryManager.run {

                    try {

                        val temp = FileUtil.copyToTemp(
                            this@HackerDashboard,
                            doc.uri
                        )

                        val uploaded = Uploader.uploadFile(
                            SERVER_URL,
                            temp
                        )

                        temp.delete()
                        uploaded

                    } catch (e: Exception) {
                        false
                    }
                }

                if (ok) success++

                val percent = ((index + 1) * 100) / total

                withContext(Dispatchers.Main) {

                    progress.progress = percent

                    if (ok)
                        log("Uploaded: ${doc.name}")
                    else
                        log("Failed: ${doc.name}")
                }
            }

            withContext(Dispatchers.Main) {
                status.text = "Backup Finished ✅"
                log("Result: $success / $total uploaded")
            }
        }
    }

    // ==================================================
    // AUTO BACKUP SCHEDULER (PHASE-2 CORE)
    // ==================================================

    private fun startAutoBackup() {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi
            .setRequiresCharging(true)                     // Charging
            .build()

        val work = PeriodicWorkRequestBuilder<BackupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "AUTO_BACKUP",
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )

        log("Auto backup scheduled (daily) ✅")
        status.text = "Auto Backup ON"
    }

    // ==================================================
    // LOG SYSTEM
    // ==================================================

    private fun log(msg: String) {
        runOnUiThread {
            logBox.append("➤ $msg\n")
        }
    }

    // ==================================================
    // CLEANUP
    // ==================================================

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}