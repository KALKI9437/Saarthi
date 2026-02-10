package com.saarthi.app.backup

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class BackupWorker(
    val ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {

    override fun doWork(): Result {

        return try {

            AutoBackupManager.run(ctx)

            Result.success()

        } catch (e: Exception) {

            e.printStackTrace()
            Result.retry()
        }
    }
}