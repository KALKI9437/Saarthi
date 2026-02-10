package com.saarthi.app.data

import android.content.Context

object HashManager {

    private const val NAME = "file_hashes"

    fun save(
        ctx: Context,
        path: String,
        hash: String
    ) {

        ctx.getSharedPreferences(NAME, 0)
            .edit()
            .putString(path, hash)
            .apply()
    }

    fun get(
        ctx: Context,
        path: String
    ): String? {

        return ctx
            .getSharedPreferences(NAME, 0)
            .getString(path, null)
    }
}