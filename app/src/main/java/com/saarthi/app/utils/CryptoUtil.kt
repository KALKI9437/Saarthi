package com.saarthi.app.utils

import android.util.Base64

object CryptoUtil {

    fun encrypt(text: String): String {

        return Base64.encodeToString(
            text.toByteArray(),
            Base64.DEFAULT
        )
    }

    fun decrypt(text: String): String {

        return String(
            Base64.decode(
                text,
                Base64.DEFAULT
            )
        )
    }
}