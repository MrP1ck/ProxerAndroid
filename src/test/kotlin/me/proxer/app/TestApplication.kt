package me.proxer.app

import android.app.Application

/**
 * Replaces [MainApplication] in Robolectric tests, which would otherwise initialize Koin, WorkManager and other
 * libraries that are not needed (and partly not working) there.
 */
class TestApplication : Application()
