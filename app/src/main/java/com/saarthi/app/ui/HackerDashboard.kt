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
import com.saarthi.app.data.HashManager
import com.saarthi.app.security.CryptoUtil
import com.saarthi.app.security.KeyManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class HackerDashboard : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var logBox: TextView
    private lateinit var progress: ProgressBar

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val PICK_FOLDER = 101
    private var selectedFolder: Uri? = null

    private val SERVER_URL = "http://YOUR_SERVER/upload"
    private val HEALTH_URL = "http://YOUR_SERVER/health"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hacker)

        StoragePermission.request(this)

        status = findViewById(R.id.statusText)
        logBox = findViewById(R.id.logBox)
        progress = findViewById(R.id.progressBar)

        PrefManager.saveServer(this, SERVER_URL)

        findViewById<Button>(R.id.btnConnect).setOnClickListener { testConnection() }
        findViewById<Button>(R.id.btnScan).setOnClickListener { pickFolder() }
        findViewById<Button>(R.id.btnUpload).setOnClickListener { startBackup() }

        findViewById<Button>(R.id.btnAuto).setOnClickListener {
            if (PrefManager.getFolder(this) == null) {
                log("❗ Select folder first")
                Toast.makeText(this, "Select folder first", Toast.LENGTH_SHORT).show()
            } else {
                startAutoBackup()
            }
        }

        log("Dashboard Loaded ✅")
    }

    // ==================================================
    // SERVER CHECK
    // ==================================================

    private fun testConnection() {
        status.text = "Connecting..."
        scope.launch {
            try {
                val req = Request.Builder().url(HEALTH_URL).build()
                val res = OkHttpClient().newCall(req).execute()

                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        status.text = "Connected ✅"
                        log("Server OK")
                    } else {
                        status.text = "Server Error ❌"
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
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        i.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
        startActivityForResult(i, PICK_FOLDER)
    }

    override fun onActivityResult(rc: Int, res: Int, data: Intent?) {
        super.onActivityResult(rc, res, data)

        if (rc == PICK_FOLDER && res == Activity.RESULT_OK) {
            selectedFolder = data?.data
            if (selectedFolder != null) {
                contentResolver.takePersistableUriPermission(
                    selectedFolder!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                PrefManager.saveFolder(this, selectedFolder.toString())
                status.text = "Folder Ready"
                log("Folder Selected & Saved ✅")
            }
        }
    }

    // ==================================================
    // MANUAL BACKUP (PHASE-4 SECURE)
    // ==================================================

    private fun startBackup() {

        if (selectedFolder == null) {
            log("❗ Select folder first")
            return
        }

        status.text = "Backing up..."
        progress.progress = 0

        scope.launch {

            val files = FileScanner.scanFolder(this@HackerDashboard, selectedFolder!!)
            if (files.isEmpty()) {
                withContext(Dispatchers.Main) {
                    log("No files found")
                    status.text = "Nothing to backup"
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
                        val temp = FileUtil.copyToTemp(this@HackerDashboard, doc.uri)

                        // 🔐 Encrypt file
                        val encrypted = File(temp.parent, temp.name + ".enc")
                        CryptoUtil.encrypt(temp, encrypted, key)
                        temp.delete()

                        // 🔍 Hash check on encrypted file
                        val hash = HashUtil.getHash(encrypted)
                        val oldHash = HashManager.get(this@HackerDashboard, doc.name)

                        if (hash == oldHash) {
                            encrypted.delete()
                            skipped++
                            withContext(Dispatchers.Main) {
                                log("Skipped (No Change): ${doc.name}")
                            }
                            return@run true
                        }

                        val uploaded = Uploader.uploadFile(SERVER_URL, encrypted)

                        if (uploaded) {
                            HashManager.save(this@HackerDashboard, doc.name, hash)
                        }

                        encrypted.delete()
                        uploaded

                    } catch (e: Exception) {
                        false
                    }
                }

                if (ok) success++

                withContext(Dispatchers.Main) {
                    progress.progress = ((i + 1) * 100) / total
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

        val work = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AUTO_BACKUP",
            ExistingPeriodicWorkPolicy.REPLACE,
            work
        )

        status.text = "Auto Backup ON"
        log("Auto backup scheduled ✅")
    }

    // ==================================================
    // LOG
    // ==================================================

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