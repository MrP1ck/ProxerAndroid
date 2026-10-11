package me.proxer.app.base

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import me.proxer.app.auth.LocalUser
import me.proxer.app.util.Validators
import me.proxer.app.util.data.EventBus
import me.proxer.app.util.data.InstantJsonAdapter
import me.proxer.app.util.data.LocalDataInitializer
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.threeten.bp.Instant

/**
 * Provides the dependencies of the base ViewModels (with plain SharedPreferences instead of the encrypted ones) and
 * runs coroutines on the main dispatcher immediately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTestRule : TestWatcher() {

    lateinit var bus: EventBus
        private set

    lateinit var storageHelper: StorageHelper
        private set

    lateinit var preferenceHelper: PreferenceHelper
        private set

    override fun starting(description: Description) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("test_preferences", Context.MODE_PRIVATE)
        val storagePreferences = context.getSharedPreferences("test_storage", Context.MODE_PRIVATE)

        preferences.edit().clear().putInt("version", 7).commit()
        storagePreferences.edit().clear().commit()

        val initializer = LocalDataInitializer(context, preferences, storagePreferences)
        val moshi = Moshi.Builder().add(Instant::class.java, InstantJsonAdapter()).build()

        bus = EventBus()
        preferenceHelper = PreferenceHelper(initializer, preferences)
        storageHelper = StorageHelper(initializer, storagePreferences, moshi)

        Dispatchers.setMain(UnconfinedTestDispatcher())

        startKoin {
            modules(
                module {
                    single { bus }
                    single { preferenceHelper }
                    single { storageHelper }
                    single { Validators(storageHelper, preferenceHelper) }
                }
            )
        }
    }

    override fun finished(description: Description) {
        stopKoin()
        Dispatchers.resetMain()
    }

    fun logIn() {
        storageHelper.user = LocalUser(token = "token", id = "1", name = "name", image = "")
    }

    fun logOut() {
        storageHelper.user = null
    }
}

fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()
