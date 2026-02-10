package com.saarthi.app.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.saarthi.app.data.PrefManager
import com.saarthi.app.data.HashManager

object AutoBackupManager {

    fun run(ctx: Context) {

        // Get saved folder
        val uriStr = PrefManager.getFolder(ctx) ?: return

        val uri = Uri.parse(uriStr)

        val folder = DocumentFile.fromTreeUri(ctx, uri) ?: return

        val files = FileScanner.scanDocument(folder)

        for (f in files) {

            try {

                val temp = FileUtil.copyToTemp(ctx, f.uri)

                // 🔥 STEP-5.1: Generate hash
                val hash = HashUtil.getHash(temp)

                // 🔥 STEP-5.2: Get old hash
                val oldHash = HashManager.get(
                    ctx,
                    f.name ?: ""
                )

                // 🔁 STEP-5.3: Compare (Skip if same)
                if (hash == oldHash) {

                    temp.delete()
                    continue
                }

                // 🚀 STEP-5.4: Upload only if changed
                val uploaded = Uploader.uploadFile(
                    PrefManager.getServer(ctx),
                    temp
                )

                // 💾 STEP-5.5: Save new hash
                if (uploaded) {

                    HashManager.save(
                        ctx,
                        f.name ?: "",
                        hash
                    )
                }

                temp.delete()

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }
}