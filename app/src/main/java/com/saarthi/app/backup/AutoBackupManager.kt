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

        val uriStr = PrefManager.getFolder(ctx) ?: return
        val server = PrefManager.getServer(ctx) ?: return

        val uri = Uri.parse(uriStr)

        val folder = DocumentFile.fromTreeUri(ctx, uri) ?: return

        val files = FileScanner.scanFolder(ctx, folder.uri)

        if (files.isEmpty()) return

        val key = KeyManager.getOrCreate(ctx)

        for (f in files) {

            try {

                val temp = FileUtil.copyToTemp(ctx, f.uri)

                val encrypted = File(
                    temp.parent,
                    temp.name + ".enc"
                )

                CryptoUtil.encrypt(
                    temp,
                    encrypted,
                    key
                )

                temp.delete()

                val hashBytes = HashUtil.getHash(encrypted)
val hash = hashBytes.joinToString("") { "%02x".format(it) }

val name = f.name ?: f.uri.toString()

val oldHash = HashManager.get(ctx, name)

if (hash == oldHash) {
    encrypted.delete()
    continue
}

                // ✅ FIXED CALLBACK
                val uploaded = Uploader.uploadFile(
                    server,
                    encrypted
                ) { 
                    // Auto mode → no UI → ignore progress
                }

                if (uploaded) {

                    HashManager.save(
                        ctx,
                        name,
                        hash
                    )
                }

                encrypted.delete()

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }
}