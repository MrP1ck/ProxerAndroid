package me.proxer.app.widget

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import me.proxer.app.R
import me.proxer.app.ui.theme.ClassicDarkColorScheme
import me.proxer.app.ui.theme.ClassicLightColorScheme

/**
 * The theme of the widgets: the dynamic colors of the system on Android 12 and later, the classic colors before.
 * [isDark] forces the dark colors, which the dark variants of the widgets use.
 */
@Composable
fun ProxerGlanceTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current

    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark -> ColorProviders(dynamicDarkColorScheme(context))
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> GlanceTheme.colors
        isDark -> ColorProviders(ClassicDarkColorScheme)
        else -> ColorProviders(light = ClassicLightColorScheme, dark = ClassicDarkColorScheme)
    }

    GlanceTheme(colors = colors, content = content)
}

/**
 * A widget with a title bar (opening [onTitleClick] and with a refresh button) and a list of [items], or the loading
 * or error state of [state].
 */
@Composable
fun <T> ProxerWidgetContent(
    title: String,
    state: WidgetState<T>?,
    onTitleClick: Action,
    onRefresh: Action,
    itemKey: (T) -> Long,
    itemContent: @Composable (T) -> Unit
) {
    Scaffold(
        titleBar = {
            TitleBar(
                startIcon = ImageProvider(R.drawable.ic_proxer),
                title = title,
                modifier = GlanceModifier.clickable(onTitleClick),
                actions = {
                    CircleIconButton(
                        imageProvider = ImageProvider(R.drawable.ic_symbol_refresh),
                        contentDescription = LocalContext.current.getString(R.string.error_action_retry),
                        onClick = onRefresh,
                        backgroundColor = null
                    )
                }
            )
        },
        backgroundColor = GlanceTheme.colors.widgetBackground
    ) {
        val error = state?.error

        when {
            state == null || (state.isLoading && state.items.isEmpty()) -> Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GlanceTheme.colors.primary)
            }
            error != null && state.items.isEmpty() -> WidgetErrorContent(error)
            else -> LazyColumn(GlanceModifier.fillMaxSize()) {
                items(state.items, itemId = itemKey) { item ->
                    Column(GlanceModifier.fillMaxWidth()) {
                        itemContent(item)

                        Spacer(GlanceModifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * A card of an item in a widget list.
 */
@Composable
fun WidgetItem(title: String, subtitle: String, onClick: Action, overline: String? = null) {
    Column(
        GlanceModifier
            .fillMaxWidth()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(onClick)
    ) {
        if (overline != null) {
            Text(
                text = overline,
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
        }

        Text(
            text = title,
            maxLines = 2,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        )

        Text(
            text = subtitle,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        )
    }
}

@Composable
private fun WidgetErrorContent(error: WidgetError) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = error.message,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, textAlign = TextAlign.Center)
        )

        if (error.buttonText != null && error.url != null) {
            Spacer(GlanceModifier.height(12.dp))

            Button(
                text = error.buttonText,
                onClick = actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(error.url))),
                colors = ButtonDefaults.buttonColors()
            )
        }
    }
}
