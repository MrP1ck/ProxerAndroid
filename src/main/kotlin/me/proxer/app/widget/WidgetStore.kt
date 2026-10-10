package me.proxer.app.widget

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.Utils
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.koin.core.context.GlobalContext
import timber.log.Timber

/**
 * The state of a widget, shared by all instances of it: the [items], whether it [isLoading] or an [error].
 */
data class WidgetState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val error: WidgetError? = null
)

/**
 * An error shown in a widget. The texts are stored resolved, since resource ids change between versions. If [url] is
 * set, the button opens it (e.g. to solve a captcha).
 */
@JsonClass(generateAdapter = true)
data class WidgetError(val message: String, val buttonText: String?, val url: String?) {

    companion object {
        fun from(context: Context, action: ErrorAction) = WidgetError(
            message = context.getString(action.message),
            buttonText = when (action.buttonAction) {
                ButtonAction.CAPTCHA -> context.getString(R.string.error_action_captcha)
                else -> null
            },
            url = when (action.buttonAction) {
                ButtonAction.CAPTCHA -> ProxerUrls.captchaWeb(Utils.getIpAddress(), Device.MOBILE).toString()
                else -> null
            }
        )
    }
}

@JsonClass(generateAdapter = true)
internal data class StoredWidgetState(val items: List<String>, val isLoading: Boolean, val error: WidgetError?)

/**
 * Stores the [WidgetState] of the widgets as JSON in shared preferences, so the widgets can be rendered at any time.
 */
class WidgetStore<T>(private val key: String, private val itemClass: Class<T>) {

    private companion object {
        private const val PREFERENCES_NAME = "widgets"
    }

    private val moshi get() = GlobalContext.get().get<Moshi>()

    private var flow: MutableStateFlow<WidgetState<T>?>? = null

    /**
     * The state as a flow, which emits whenever it is [written][write]. Used by the widgets to recompose.
     */
    fun flow(context: Context): StateFlow<WidgetState<T>?> = synchronized(this) {
        flow ?: MutableStateFlow(read(context)).also { flow = it }
    }

    fun read(context: Context): WidgetState<T>? {
        val json = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getString(key, null)
            ?: return null

        return try {
            val itemAdapter = moshi.adapter(itemClass)

            moshi.adapter(StoredWidgetState::class.java).fromJson(json)?.let { stored ->
                WidgetState(stored.items.mapNotNull { itemAdapter.fromJson(it) }, stored.isLoading, stored.error)
            }
        } catch (error: Exception) {
            Timber.e(error)

            null
        }
    }

    fun write(context: Context, state: WidgetState<T>) {
        val itemAdapter = moshi.adapter(itemClass)
        val stored = StoredWidgetState(state.items.map { itemAdapter.toJson(it) }, state.isLoading, state.error)

        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putString(key, moshi.adapter(StoredWidgetState::class.java).toJson(stored))
        }

        synchronized(this) { flow?.value = state }
    }
}
