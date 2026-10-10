package me.proxer.app.util.extension

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Shows the [notification] if the user allowed notifications (required since Android 13).
 */
fun Context.notifyIfPermitted(id: Int, notification: Notification) {
    val isPermitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    if (isPermitted) {
        NotificationManagerCompat.from(this).notify(id, notification)
    }
}
