package com.saarthi.app.backup

import java.io.File
import java.security.MessageDigest

object HashUtil {

    fun getHash(file: File): String {

        val md = MessageDigest.getInstance("SHA-256")

        file.inputStream().use { input ->

            val buffer = ByteArray(1024)
            var read: Int

            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }

        return md.digest()
            .joinToString("") { "%02x".format(it) }
    }
}