package com.almendras.scrolly.core.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object MediaPermissions {

    /** Permiso de lectura de videos según la versión de Android. */
    val videoPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasVideoPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, videoPermission) ==
            PackageManager.PERMISSION_GRANTED
}
