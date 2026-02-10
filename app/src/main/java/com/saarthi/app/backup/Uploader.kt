package com.saarthi.app.backup

import okhttp3.*
import java.io.File

object Uploader {

    private val client = OkHttpClient()

    fun uploadFile(url: String, file: File): Boolean {

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody()
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val res = client.newCall(request).execute()

        return res.isSuccessful
    }
}