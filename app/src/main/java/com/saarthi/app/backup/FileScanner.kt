package com.saarthi.app.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object FileScanner {

    fun scanFolder(ctx: Context, uri: Uri): List<ScannedDocument> {

        val folder = DocumentFile.fromTreeUri(ctx, uri)
            ?: return emptyList()

        return folder.listFiles().mapNotNull { file ->

            if (!file.isFile) return@mapNotNull null

            val name = file.name ?: "unknown"

            ScannedDocument(
                name = name,
                uri = file.uri
            )
        }
    }
}