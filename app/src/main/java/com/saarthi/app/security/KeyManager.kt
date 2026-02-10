package com.saarthi.app.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

object KeyManager {

    private const val NAME = "secure_keys"

    fun getOrCreate(ctx: Context): String {

        val pref = ctx.getSharedPreferences(NAME, 0)
        val existing = pref.getString("key", null)

        if (existing != null) return existing

        val key = ByteArray(32)
        SecureRandom().nextBytes(key)

        val encoded = Base64.encodeToString(key, Base64.NO_WRAP)

        pref.edit().putString("key", encoded).apply()

        return encoded
    }
}