package com.saarthi.app.data

import android.content.Context

object PrefManager {

    private const val NAME = "auto_backup"

    private const val KEY_FOLDER = "folder"
    private const val KEY_SERVER = "server"
    private const val KEY_LAST_BACKUP = "last_backup"

    // ================= FOLDER =================

    fun saveFolder(ctx: Context, uri: String) {

        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FOLDER, uri)
            .apply()
    }

    fun getFolder(ctx: Context): String? {

        return ctx
            .getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER, null)
    }

    // ================= SERVER =================

    fun saveServer(ctx: Context, url: String) {

        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER, url)
            .apply()
    }

    fun getServer(ctx: Context): String {

        return ctx
            .getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_SERVER, "") ?: ""
    }

    // ================= LAST BACKUP TIME =================

    // 🔥 Save last backup timestamp
    fun setLastBackup(ctx: Context, time: String) {

        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_BACKUP, time)
            .apply()
    }

    // 🔥 Get last backup timestamp
    fun getLastBackup(ctx: Context): String? {

        return ctx
            .getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_BACKUP, null)
    }

    // ================= CLEAR (OPTIONAL) =================

    fun clearAll(ctx: Context) {

        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}