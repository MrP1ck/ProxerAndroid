package me.proxer.app.util.data

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onStart

/**
 * Emits the value read by [read] whenever the entry for [key] changed to a different value. Does not emit the current
 * value on collection.
 */
fun <T> SharedPreferences.changes(key: String, read: SharedPreferences.() -> T): Flow<T> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, changedKey ->
        if (changedKey == key) trySend(preferences.read())
    }

    registerOnSharedPreferenceChangeListener(listener)

    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.onStart { emit(read()) }.distinctUntilChanged().drop(1)
