package com.saarthi.app.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.saarthi.app.data.PrefManager

object AutoBackupManager {

    fun run(ctx: Context) {

        val uriStr = PrefManager.getFolder(ctx) ?: return

        val uri = Uri.parse(uriStr)

        val folder = DocumentFile.fromTreeUri(ctx, uri) ?: return

        val files = FileScanner.scanDocument(folder)

        for (f in files) {

            val temp = FileUtil.copyToTemp(ctx, f.uri)

            Uploader.uploadFile(
                PrefManager.getServer(ctx),
                temp
            )

            temp.delete()
        }
    }
}