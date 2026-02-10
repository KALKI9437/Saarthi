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
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HackerDashboard : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var logBox: TextView
    private lateinit var progress: ProgressBar

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val PICK_FOLDER = 101
    private var selectedFolder: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hacker)

        // Storage permission
        StoragePermission.request(this)

        status = findViewById(R.id.statusText)
        logBox = findViewById(R.id.logBox)
        progress = findViewById(R.id.progressBar)

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            testConnection()
        }

        // Phase-1: Folder picker
        findViewById<Button>(R.id.btnScan).setOnClickListener {
            pickFolder()
        }

        // Phase-1: Real backup
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            startBackup()
        }

        log("Hacker Dashboard Loaded")
    }

    // ---------------- CONNECT SERVER ----------------

    private fun testConnection() {

        status.text = "Connecting..."
        log("Trying to connect to server...")

        scope.launch {
            try {
                val request = Request.Builder()
                    .url("http://YOUR_SERVER/health") // optional health API
                    .build()

                val response = OkHttpClient().newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        status.text = "Connected ✅"
                        log("Server connected successfully")
                    } else {
                        status.text = "Server Error ❌"
                        log("Server responded but not OK")
                    }
                }

                response.close()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status.text = "Failed ❌"
                    log("Connection failed")
                }
            }
        }
    }

    // ---------------- PICK FOLDER ----------------

    private fun pickFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
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
            log("Folder selected ✅")
            status.text = "Folder Ready"
        }
    }

    // ---------------- REAL BACKUP ENGINE ----------------

    private fun startBackup() {

        if (selectedFolder == null) {
            log("Select folder first ❗")
            return
        }

        status.text = "Backing up..."
        progress.progress = 0

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

            var success = 0
            val total = files.size

            for ((index, doc) in files.withIndex()) {
                try {
                    val tempFile = FileUtil.copyToTemp(
                        this@HackerDashboard,
                        doc.uri
                    )

                    val ok = Uploader.uploadFile(
                        "http://YOUR_SERVER/upload",
                        tempFile
                    )

                    tempFile.delete()

                    if (ok) success++

                    val percent = ((index + 1) * 100) / total
                    withContext(Dispatchers.Main) {
                        progress.progress = percent
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                status.text = "Backup Finished ✅"
                log("Uploaded $success / $total files")
            }
        }
    }

    // ---------------- LOG SYSTEM ----------------

    private fun log(msg: String) {
        runOnUiThread {
            logBox.append("➤ $msg\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
