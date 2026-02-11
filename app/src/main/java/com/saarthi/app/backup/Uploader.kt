package com.saarthi.app.backup

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

// 🔹 Progress Enabled Body
class ProgressRequestBody(
    private val file: File,
    private val callback: ((Int) -> Unit)?   // ✅ Optional
) : RequestBody() {

    override fun contentType(): MediaType? {
        return "application/octet-stream".toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return file.length()
    }

    override fun writeTo(sink: BufferedSink) {

        val buffer = ByteArray(2048)
        val input = FileInputStream(file)

        var uploaded: Long = 0
        val total = file.length()

        input.use {

            var read: Int

            while (it.read(buffer).also { read = it } != -1) {

                uploaded += read
                sink.write(buffer, 0, read)

                val percent = (uploaded * 100 / total).toInt()

                // ✅ Safe callback
                callback?.invoke(percent)
            }
        }
    }
}

// 🔹 Main Uploader
object Uploader {

    private val client = OkHttpClient()

    fun uploadFile(
        url: String,
        file: File,
        progress: ((Int) -> Unit)? = null   // ✅ Default = null
    ): Boolean {

        val body = ProgressRequestBody(file, progress)

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, body)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(multipart)
            .build()

        val response = client.newCall(request).execute()

        return response.isSuccessful
    }
}