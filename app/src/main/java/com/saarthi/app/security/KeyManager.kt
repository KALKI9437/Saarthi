package com.saarthi.app.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

object KeyManager {

    private const val NAME = "secure_keys"
    private const val KEY_NAME = "key"

    // 🔐 Returns RAW ByteArray (For Encryption)
    fun getOrCreate(ctx: Context): ByteArray {

        val pref = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

        val existing = pref.getString(KEY_NAME, null)

        // If key already exists → decode & return
        if (existing != null) {

            return Base64.decode(existing, Base64.NO_WRAP)
        }

        // Generate new 256-bit key
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)

        // Store as Base64
        val encoded = Base64.encodeToString(key, Base64.NO_WRAP)

        pref.edit()
            .putString(KEY_NAME, encoded)
            .apply()

        return key
    }

    // 🔥 Optional: Reset key (for testing / logout)
    fun clear(ctx: Context) {

        val pref = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

        pref.edit()
            .remove(KEY_NAME)
            .apply()
    }
}