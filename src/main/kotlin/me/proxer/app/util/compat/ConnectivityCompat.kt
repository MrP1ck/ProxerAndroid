@file:Suppress("DEPRECATION")

package me.proxer.app.util.compat

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

val ConnectivityManager.isConnected
    get() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> activeNetwork != null
        else -> activeNetworkInfo != null
    }

val ConnectivityManager.isConnectedToCellular
    get() = getNetworkCapabilities(activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
