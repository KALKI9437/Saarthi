package com.saarthi.app.data

import android.content.Context

class LocalCache(ctx: Context) {

    private val prefs =
        ctx.getSharedPreferences(
            "cache",
            Context.MODE_PRIVATE
        )

    fun save(key: String, value: String) {

        prefs.edit()
            .putString(key, value)
            .apply()
    }

    fun load(key: String): String? {

        return prefs.getString(key, null)
    }
}