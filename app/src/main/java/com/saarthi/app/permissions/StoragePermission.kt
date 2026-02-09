package com.saarthi.app.permissions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object StoragePermission {

    fun request(activity: Activity) {

        if (!android.os.Environment.isExternalStorageManager()) {

            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )

            activity.startActivity(intent)
        }
    }
}
