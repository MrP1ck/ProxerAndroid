package me.proxer.app.ui.image

import android.content.Context
import android.os.Build
import android.os.Environment
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import me.proxer.app.BuildConfig
import me.proxer.app.util.data.PreferenceHelper
import okhttp3.OkHttpClient

/**
 * Creates the Coil [ImageLoader] used by Compose. It shares the [OkHttpClient] (and with that the interceptors and
 * cache configuration) with the rest of the app, like the Glide setup in ProxerGlideModule.
 */
object ProxerImageLoader {

    private const val CACHE_SIZE = 1_024L * 1_024L * 250L
    private const val CACHE_DIR = "coil"

    fun create(context: Context, client: OkHttpClient, preferenceHelper: PreferenceHelper): ImageLoader {
        val cacheDir = when (!Environment.isExternalStorageEmulated() && preferenceHelper.shouldCacheExternally) {
            true -> context.externalCacheDir ?: context.cacheDir
            false -> context.cacheDir
        }

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { client }))

                // Emoticons in forum posts and some images are animated GIFs.
                when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    true -> add(AnimatedImageDecoder.Factory())
                    false -> add(GifDecoder.Factory())
                }
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve(CACHE_DIR))
                    .maxSizeBytes(CACHE_SIZE)
                    .build()
            }
            .crossfade(true)
            .apply { if (BuildConfig.DEBUG) logger(coil3.util.DebugLogger()) }
            .build()
    }
}
