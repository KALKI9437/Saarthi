package com.saarthi.app.ui

import android.app.AlertDialog
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HackerDashboard : AppCompatActivity() {

    // ================= UI =================
    private lateinit var statusText: TextView
    private lateinit var logBox: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnConnect: Button
    private lateinit var btnScan: Button
    private lateinit var btnUpload: Button
    private lateinit var btnAuto: Button
    private lateinit var btnStopAuto: Button
    private lateinit var btnClearLogs: Button
    private lateinit var txtFileCount: TextView
    private lateinit var txtLastBackup: TextView
    private lateinit var txtServerStatus: TextView

    private var selectedFolder: Uri? = null

    // ================= CONFIG =================
    private val SERVER_URL = "http://192.168.1.5:8000/upload"
    private val HEALTH_URL = "http://192.168.1.5:8000/health"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ================= FOLDER PICKER =================
    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                log("❌ Folder selection cancelled")
                return@registerForActivityResult
            }

            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            selectedFolder = uri
            PrefManager.saveFolder(this, uri.toString())

            lifecycleScope.launch(Dispatchers.IO) {
                val files = FileScanner.scanFolder(this@HackerDashboard, uri)
                withContext(Dispatchers.Main) {
                    txtFileCount.text = "📁 Files: ${files.size}"
                    statusText.text = "✅ Folder Ready"
                    log("Folder selected (${files.size} files)")
                }
            }
        }

    // ================= ON CREATE =================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hacker)

        StoragePermission.request(this)
        initViews()
        PrefManager.saveServer(this, SERVER_URL)
        loadSavedState()
        setupButtons()
        lifecycleScope.launch { checkServerStatus() }

        log("🚀 Saarthi Backup Dashboard Loaded")
        log("✅ Phase-1,2,3,4 Active")
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        logBox = findViewById(R.id.logBox)
        progressBar = findViewById(R.id.progressBar)

        btnConnect = findViewById(R.id.btnConnect)
        btnScan = findViewById(R.id.btnScan)
        btnUpload = findViewById(R.id.btnUpload)
        btnAuto = findViewById(R.id.btnAuto)
        btnStopAuto = findViewById(R.id.btnStopAuto)
        btnClearLogs = findViewById(R.id.btnClearLogs)

        txtFileCount = findViewById(R.id.fileCountText)
        txtLastBackup = findViewById(R.id.lastBackupText)
        txtServerStatus = findViewById(R.id.serverStatusText)
    }

    private fun loadSavedState() {
        PrefManager.getFolder(this)?.let {
            selectedFolder = Uri.parse(it)
            statusText.text = "📁 Previous folder loaded"
        }

        txtLastBackup.text =
            "Last Backup: ${PrefManager.getLastBackup(this) ?: "Never"}"
    }

    // ================= BUTTONS =================
    private fun setupButtons() {
        btnConnect.setOnClickListener { testServerConnection() }
        btnScan.setOnClickListener { folderPicker.launch(null) }
        btnUpload.setOnClickListener { startManualBackup() }
        btnAuto.setOnClickListener { startAutoBackup() }
        btnStopAuto.setOnClickListener { stopAutoBackup() }
        btnClearLogs.setOnClickListener { logBox.text = "" }
    }

    // ================= PHASE-1 + 3 + 4 =================
    private fun startManualBackup() {
        val folder = selectedFolder ?: run {
            toast("Select folder first")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val files = FileScanner.scanFolder(this@HackerDashboard, folder)
            if (files.isEmpty()) return@launch

            val key = KeyManager.getOrCreate(this@HackerDashboard)
            var success = 0
            var skipped = 0
            var failed = 0

            for ((i, doc) in files.withIndex()) {
                val result = processFile(doc, key, i + 1, files.size)
                when (result) {
                    "SUCCESS" -> success++
                    "SKIPPED" -> skipped++
                    else -> failed++
                }
            }

            withContext(Dispatchers.Main) {
                val ts = timestamp()
                PrefManager.setLastBackup(this@HackerDashboard, ts)
                txtLastBackup.text = "Last Backup: $ts"
                showSummary(success, skipped, failed, files.size)
            }
        }
    }

    private suspend fun processFile(
        doc: ScannedDocument,
        key: ByteArray,
        index: Int,
        total: Int
    ): String = try {

        val temp = FileUtil.copyToTemp(this, doc.uri) ?: return "FAILED"
        val enc = File(temp.parent, temp.name + ".enc")

        CryptoUtil.encrypt(temp, enc, key)
        temp.delete()

        val hash = HashUtil.getHash(enc)
        val old = HashManager.get(this, doc.uri.toString())

        if (hash == old) {
            enc.delete()
            log("⏭ Skipped: ${doc.name}")
            return "SKIPPED"
        }

        val uploaded = Uploader.uploadFile(SERVER_URL, enc)
        if (uploaded) {
            HashManager.save(this, doc.uri.toString(), hash)
            log("✅ Uploaded: ${doc.name}")
            "SUCCESS"
        } else "FAILED"

    } catch (e: Exception) {
        log("❌ ${doc.name}: ${e.message}")
        "FAILED"
    }

    // ================= PHASE-2 =================
    private fun startAutoBackup() {
        val work = PeriodicWorkRequestBuilder<BackupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "SAARTHI_AUTO_BACKUP",
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )

        log("🤖 Auto backup enabled")
    }

    private fun stopAutoBackup() {
        WorkManager.getInstance(this)
            .cancelUniqueWork("SAARTHI_AUTO_BACKUP")
        log("⏹ Auto backup stopped")
    }

    // ================= SERVER =================
    private fun testServerConnection() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val res = http.newCall(
                    Request.Builder().url(HEALTH_URL).build()
                ).execute()

                withContext(Dispatchers.Main) {
                    txtServerStatus.text =
                        if (res.isSuccessful) "🟢 ONLINE" else "🔴 OFFLINE"
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    txtServerStatus.text = "🔴 OFFLINE"
                }
            }
        }
    }

    private suspend fun checkServerStatus() {
        testServerConnection()
    }

    // ================= HELPERS =================
    private fun log(msg: String) {
        runOnUiThread {
            logBox.append("[${time()}] $msg\n")
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun timestamp(): String =
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

    private fun time(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    private fun showSummary(s: Int, sk: Int, f: Int, t: Int) {
        AlertDialog.Builder(this)
            .setTitle("Backup Summary")
            .setMessage(
                "Success: $s\nSkipped: $sk\nFailed: $f\nTotal: $t"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}