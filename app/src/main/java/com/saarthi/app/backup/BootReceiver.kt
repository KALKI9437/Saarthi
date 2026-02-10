package com.saarthi.app.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            val work = PeriodicWorkRequestBuilder<BackupWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "AUTO_BACKUP",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    work
                )
        }
    }
}