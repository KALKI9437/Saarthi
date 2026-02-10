package com.saarthi.app.backup

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtil {

    fun copyToTemp(ctx: Context, uri: Uri): File {

        val input = ctx.contentResolver.openInputStream(uri)!!

        val file = File(ctx.cacheDir, "temp_${System.currentTimeMillis()}")

        val out = FileOutputStream(file)

        input.copyTo(out)

        input.close()
        out.close()

        return file
    }
}