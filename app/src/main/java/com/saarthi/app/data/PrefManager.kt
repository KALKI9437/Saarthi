package com.saarthi.app.data

import android.content.Context

object PrefManager {

    private const val NAME = "auto_backup"

    fun saveFolder(ctx: Context, uri: String) {

        ctx.getSharedPreferences(NAME, 0)
            .edit()
            .putString("folder", uri)
            .apply()
    }

    fun getFolder(ctx: Context): String? {

        return ctx
            .getSharedPreferences(NAME, 0)
            .getString("folder", null)
    }

    fun saveServer(ctx: Context, url: String) {

        ctx.getSharedPreferences(NAME, 0)
            .edit()
            .putString("server", url)
            .apply()
    }

    fun getServer(ctx: Context): String {

        return ctx
            .getSharedPreferences(NAME, 0)
            .getString("server", "")!!
    }
}