package me.proxer.app.util

import android.app.Activity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

/**
 * Flexible in-app updates through Google Play. The UI is up to the caller: [Callbacks.onUpdateAvailable] should offer
 * to download the update, [Callbacks.onUpdateDownloaded] to install it.
 *
 * @author Ruben Gees
 */
class InAppUpdateFlow {

    companion object {
        const val REQUEST_CODE = 5276
    }

    interface Callbacks {
        fun onUpdateAvailable(startDownload: () -> Unit)
        fun onUpdateDownloaded(install: () -> Unit)
        fun onUpdateCancelled()
    }

    private var appUpdateManager: AppUpdateManager? = null

    private var successListener: OnSuccessListener<AppUpdateInfo>? = null
    private var progressListener: InstallStateUpdatedListener? = null
    private var failureListener: OnFailureListener? = null

    fun start(context: Activity, callbacks: Callbacks) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            appUpdateManager = AppUpdateManagerFactory.create(context).also { appUpdateManager ->
                val successListener = successListener(context, appUpdateManager, callbacks)
                val progressListener = progressListener(appUpdateManager, callbacks)
                val failureListener = OnFailureListener { error -> Timber.e(error) }

                this.successListener = successListener
                this.progressListener = progressListener
                this.failureListener = failureListener

                appUpdateManager.appUpdateInfo.addOnSuccessListener(successListener)
                appUpdateManager.appUpdateInfo.addOnFailureListener(failureListener)
                appUpdateManager.registerListener(progressListener)
            }
        }
    }

    private fun successListener(
        context: Activity,
        appUpdateManager: AppUpdateManager,
        callbacks: Callbacks
    ) = OnSuccessListener<AppUpdateInfo> { appUpdateInfo ->
        if (
            appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        ) {
            callbacks.onUpdateAvailable {
                @Suppress("DEPRECATION")
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE, context, REQUEST_CODE)
            }
        }
    }

    private fun progressListener(appUpdateManager: AppUpdateManager, callbacks: Callbacks) =
        InstallStateUpdatedListener {
            when (it.installStatus()) {
                InstallStatus.DOWNLOADED -> callbacks.onUpdateDownloaded { appUpdateManager.completeUpdate() }
                InstallStatus.CANCELED -> callbacks.onUpdateCancelled()
                else -> Unit
            }
        }

    fun stop() {
        progressListener?.also { appUpdateManager?.unregisterListener(it) }

        appUpdateManager = null
        successListener = null
        failureListener = null
        progressListener = null
    }
}
