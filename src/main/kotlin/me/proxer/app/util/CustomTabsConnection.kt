package me.proxer.app.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Keeps a connection to the Custom Tabs provider of the device while the owner is started, so pages can be preloaded
 * and opened faster.
 */
class CustomTabsConnection(context: Context) : DefaultLifecycleObserver {

    // The browser keeps its callback (and thus this object) alive for a while, so no Activity must be referenced.
    private val context = context.applicationContext

    private var connection: CustomTabsServiceConnection? = null

    var session: CustomTabsSession? = null
        private set

    override fun onStart(owner: LifecycleOwner) {
        if (connection != null) return

        val packageName = CustomTabsClient.getPackageName(context, null) ?: return

        val newConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0L)

                session = client.newSession(null)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                session = null
            }
        }

        if (CustomTabsClient.bindCustomTabsService(context, packageName, newConnection)) {
            connection = newConnection
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        connection?.let { context.unbindService(it) }

        connection = null
        session = null
    }

    /**
     * Tells the browser that the [uri] is likely to be opened soon. Returns whether the browser accepted the hint.
     */
    fun mayLaunchUrl(uri: Uri, extras: Bundle? = null, otherLikelyUrls: List<Bundle>? = null): Boolean {
        return session?.mayLaunchUrl(uri, extras, otherLikelyUrls) == true
    }

    /**
     * Opens the [uri] in a Custom Tab, or calls [fallback] if the device has no browser supporting them.
     */
    fun open(activity: Activity, customTabsIntent: CustomTabsIntent, uri: Uri, fallback: (Activity, Uri) -> Unit) {
        val packageName = CustomTabsClient.getPackageName(activity, null)

        if (packageName == null) {
            fallback(activity, uri)
        } else {
            customTabsIntent.intent.setPackage(packageName)
            customTabsIntent.launchUrl(activity, uri)
        }
    }
}
