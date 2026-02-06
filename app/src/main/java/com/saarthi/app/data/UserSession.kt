package com.saarthi.app.data

import android.content.Context

class UserSession(ctx: Context) {

    private val prefs =
        ctx.getSharedPreferences(
            "saarthi",
            Context.MODE_PRIVATE
        )

    fun saveSession(id: String, token: String) {

        prefs.edit()
            .putString("device", id)
            .putString("token", token)
            .apply()
    }

    fun isLoggedIn(): Boolean {

        return prefs.contains("token")
    }

    fun getToken(): String {

        return prefs.getString(
            "token",
            ""
        ) ?: ""
    }

    fun getDeviceId(): String {

        return prefs.getString(
            "device",
            ""
        ) ?: ""
    }
}