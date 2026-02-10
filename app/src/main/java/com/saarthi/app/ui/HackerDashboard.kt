package com.saarthi.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.saarthi.app.R
import com.saarthi.app.permissions.StoragePermission
import com.saarthi.app.backup.*
import com.saarthi.app.data.PrefManager
import com.saarthi.app.data.HashManager
import com.saarthi.app.security.CryptoUtil
import com.saarthi.app.security.KeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class HackerDashboard : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var logBox: TextView
    private lateinit var progress: ProgressBar

    private var selectedFolder: Uri? = null

    // ⚠️ Replace with real server URL
    private val SERVER_URL = "http://192.168.1.5:8000/upload"
    private val HEALTH_URL = "http://192.168.1.5:8000/health"

    // Modern folder picker (replaces onActivityResult)
    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->

            if (uri != null) {

                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedFolder = uri
                PrefManager.saveFolder(this, uri.toString())

                status.text = "Folder Ready ✅"
                log("Folder Selected")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hacker)

        StoragePermission.request(this)

        status = findViewById(R.id.statusText)
        logBox = findViewById(R.id.logBox)
        progress = findViewById(R.id.progressBar)

        PrefManager.saveServer(this, SERVER_URL)

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            testConnection()
        }

        findViewById<Button>(R.id.btnScan).setOnClickListener {
            pickFolder()
        }

        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            startBackup()
        }

        findViewById<Button>(R.id.btnAuto).setOnClickListener {
            startAutoBackup()
        }

        log("Dashboard Loaded ✅")
    }

    // ==================================================
    // SERVER CHECK
    // ==================================================

    private fun testConnection() {

        status.text = "Connecting..."

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val req = Request.Builder()
                    .url(HEALTH_URL)
                    .build()

                val res = OkHttpClient().newCall(req).execute()

                withContext(Dispatchers.Main) {

                    if (res.isSuccessful) {

                        status.text = "Connected ✅"
                        log("Server OK")

                    } else {

                        status.text = "Server Error ❌"
                        log("Server responded: ${res.code}")
                    }
                }

                res.close()

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

        folderPicker.launch(null)
    }

    // ==================================================
    // MANUAL BACKUP
    // ==================================================

    private fun startBackup() {

        val folder = selectedFolder

        if (folder == null) {

            Toast.makeText(this, "Select folder first", Toast.LENGTH_SHORT).show()
            log("❗ No folder selected")
            return
        }

        status.text = "Backing up..."
        progress.progress = 0

        lifecycleScope.launch(Dispatchers.IO) {

            val files = FileScanner.scanFolder(this@HackerDashboard, folder)

            if (files.isEmpty()) {

                withContext(Dispatchers.Main) {

                    status.text = "Nothing to backup"
                    log("No files found")
                }

                return@launch
            }

            val key = KeyManager.getOrCreate(this@HackerDashboard)

            val total = files.size
            var success = 0
            var skipped = 0

            for ((i, doc) in files.withIndex()) {

                val ok = RetryManager.run {

                    try {

                        val temp = FileUtil.copyToTemp(
                            this@HackerDashboard,
                            doc.uri
                        )

                        // Encrypt
                        val encrypted =
                            File(temp.parent, temp.name + ".enc")

                        CryptoUtil.encrypt(temp, encrypted, key)

                        temp.delete()

                        // Hash check
                        val hash = HashUtil.getHash(encrypted)
                        val old = HashManager.get(
                            this@HackerDashboard,
                            doc.name
                        )

                        if (hash == old) {

                            encrypted.delete()
                            skipped++

                            withContext(Dispatchers.Main) {

                                log("Skipped: ${doc.name}")
                            }

                            return@run true
                        }

                        val uploaded =
                            Uploader.uploadFile(
                                SERVER_URL,
                                encrypted,
                                progress.progress
                            )

                        if (uploaded) {

                            HashManager.save(
                                this@HackerDashboard,
                                doc.name,
                                hash
                            )
                        }

                        encrypted.delete()

                        uploaded

                    } catch (e: Exception) {

                        false
                    }
                }

                if (ok) success++

                withContext(Dispatchers.Main) {

                    progress.progress =
                        ((i + 1) * 100) / total
                }
            }

            withContext(Dispatchers.Main) {

                status.text = "Backup Finished ✅"

                log("Uploaded: $success / $total")
                log("Skipped: $skipped")
            }
        }
    }

    // ==================================================
    // AUTO BACKUP
    // ==================================================

    private fun startAutoBackup() {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val work =
            PeriodicWorkRequestBuilder<BackupWorker>(
                24,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(

                "AUTO_BACKUP",
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )

        status.text = "Auto Backup ON"
        log("Auto backup scheduled")
    }

    // ==================================================
    // LOG
    // ==================================================

    private fun log(msg: String) {

        logBox.append("➤ $msg\n")
    }
}