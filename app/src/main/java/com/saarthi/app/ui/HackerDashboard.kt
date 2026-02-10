package com.saarthi.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.saarthi.app.R
import com.saarthi.app.permissions.StoragePermission
import com.saarthi.app.backup.FileScanner
import com.saarthi.app.backup.FileUtil
import com.saarthi.app.backup.Uploader
import com.saarthi.app.backup.RetryManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request

class HackerDashboard : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var logBox: TextView
    private lateinit var progress: ProgressBar

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val PICK_FOLDER = 101
    private var selectedFolder: Uri? = null

    // 🔴 CHANGE THIS TO YOUR SERVER
    private val SERVER_URL = "http://YOUR_SERVER/upload"
    private val HEALTH_URL = "http://YOUR_SERVER/health"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hacker)

        // Storage permission
        StoragePermission.request(this)

        status = findViewById(R.id.statusText)
        logBox = findViewById(R.id.logBox)
        progress = findViewById(R.id.progressBar)

        // CONNECT
        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            testConnection()
        }

        // PICK FOLDER
        findViewById<Button>(R.id.btnScan).setOnClickListener {
            pickFolder()
        }

        // START BACKUP
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            startBackup()
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

                log("Folder Selected ✅")
                status.text = "Folder Ready"
            }
        }
    }

    // ==================================================
    // REAL BACKUP ENGINE (PHASE-1 CORE)
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

                        val tempFile = FileUtil.copyToTemp(
                            this@HackerDashboard,
                            doc.uri
                        )

                        val uploaded = Uploader.uploadFile(
                            SERVER_URL,
                            tempFile
                        )

                        tempFile.delete()

                        uploaded

                    } catch (e: Exception) {

                        e.printStackTrace()
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