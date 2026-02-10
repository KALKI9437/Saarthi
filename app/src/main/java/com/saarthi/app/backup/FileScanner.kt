
package com.saarthi.app.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object FileScanner {

    fun scanFolder(ctx: Context, uri: Uri): List<DocumentFile> {

        val folder = DocumentFile.fromTreeUri(ctx, uri)

        return folder?.listFiles()?.toList() ?: emptyList()
    }
}