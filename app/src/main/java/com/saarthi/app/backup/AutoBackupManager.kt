package com.saarthi.app.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.saarthi.app.data.PrefManager
import com.saarthi.app.data.HashManager
import com.saarthi.app.security.CryptoUtil
import com.saarthi.app.security.KeyManager
import java.io.File

object AutoBackupManager {

    fun run(ctx: Context) {

        // Get saved folder
        val uriStr = PrefManager.getFolder(ctx) ?: return

        val uri = Uri.parse(uriStr)

        val folder = DocumentFile.fromTreeUri(ctx, uri) ?: return

        val files = FileScanner.scanDocument(folder)

        // Get encryption key
        val key = KeyManager.getOrCreate(ctx)

        for (f in files) {

            try {

                // Copy original file to temp
                val temp = FileUtil.copyToTemp(ctx, f.uri)

                // 🔐 Encrypt temp file
                val encrypted = File(
                    temp.parent,
                    temp.name + ".enc"
                )

                CryptoUtil.encrypt(
                    temp,
                    encrypted,
                    key
                )

                // Delete plain temp file
                temp.delete()

                // 🔍 Generate hash of encrypted file
                val hash = HashUtil.getHash(encrypted)

                // 🔍 Get old hash
                val oldHash = HashManager.get(
                    ctx,
                    f.name ?: ""
                )

                // 🔁 Skip if same
                if (hash == oldHash) {

                    encrypted.delete()
                    continue
                }

                // 🚀 Upload encrypted file
                val uploaded = Uploader.uploadFile(
                    PrefManager.getServer(ctx),
                    encrypted
                )

                // 💾 Save new hash
                if (uploaded) {

                    HashManager.save(
                        ctx,
                        f.name ?: "",
                        hash
                    )
                }

                // Delete encrypted temp
                encrypted.delete()

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }
}