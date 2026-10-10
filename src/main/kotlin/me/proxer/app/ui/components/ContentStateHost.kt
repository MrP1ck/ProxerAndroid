package me.proxer.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.preview.PreviewSurface
import me.proxer.app.ui.preview.ThemePreviews
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE

/**
 * Shows the loading indicator, the error or the [content] of a [ContentState].
 *
 * [isEmpty] decides if the data is considered empty, in which case [emptyMessage] is shown instead of [content].
 */
@Composable
fun <T> ContentStateHost(
    state: ContentState<T>,
    onErrorAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    @StringRes emptyMessage: Int = R.string.error_no_data,
    isEmpty: (T) -> Boolean = { it is Collection<*> && it.isEmpty() },
    content: @Composable (T) -> Unit
) {
    val target = when {
        // An error is only shown instead of the data if there is nothing to show yet. Errors while loading more
        // items of a list are shown at the end of the list instead (see PagedList).
        state.data != null && (state.error == null || !isEmpty(state.data)) -> state.data
        state.error != null -> state.error
        else -> Loading
    }

    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it?.javaClass },
        label = "ContentStateHost"
    ) { current ->
        when (current) {
            Loading -> LoadingState(Modifier.padding(contentPadding))
            is ErrorAction -> ErrorState(current, onErrorAction, Modifier.padding(contentPadding))
            else -> {
                @Suppress("UNCHECKED_CAST")
                val data = current as T

                if (isEmpty(data)) {
                    EmptyState(emptyMessage, Modifier.padding(contentPadding))
                } else {
                    content(data)
                }
            }
        }
    }
}

private object Loading

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(
    @StringRes message: Int,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_symbol_search_off
) {
    MessageState(icon = icon, message = stringResource(message), modifier = modifier)
}

/**
 * Full screen error with an icon matching the kind of error and an optional action button.
 */
@Composable
fun ErrorState(action: ErrorAction, onAction: (ErrorAction) -> Unit, modifier: Modifier = Modifier) {
    MessageState(
        icon = action.icon(),
        message = stringResource(action.message),
        modifier = modifier,
        buttonText = action.buttonText(),
        onButtonClick = { onAction(action) }
    )
}

/**
 * A compact error for the end of a list, e.g. when loading the next page failed.
 */
@Composable
fun InlineErrorState(action: ErrorAction, onAction: (ErrorAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(action.message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        action.buttonText()?.let { buttonText ->
            FilledTonalButton(onClick = { onAction(action) }) { Text(buttonText) }
        }
    }
}

@Composable
private fun MessageState(
    @DrawableRes icon: Int,
    message: String,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {}
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (buttonText != null) {
                Spacer(Modifier.height(24.dp))

                FilledTonalButton(onClick = onButtonClick) { Text(buttonText) }
            }
        }
    }
}

@DrawableRes
private fun ErrorAction.icon() = when (buttonAction) {
    ButtonAction.NETWORK_SETTINGS -> R.drawable.ic_symbol_cloud_off
    ButtonAction.LOGIN -> R.drawable.ic_symbol_login
    ButtonAction.AGE_CONFIRMATION -> R.drawable.ic_symbol_no_adult_content
    ButtonAction.OPEN_LINK -> R.drawable.ic_symbol_open_in_new
    else -> R.drawable.ic_symbol_error
}

@Composable
private fun ErrorAction.buttonText() = when (buttonMessage) {
    ACTION_MESSAGE_HIDE -> null
    ACTION_MESSAGE_DEFAULT -> stringResource(R.string.error_action_retry)
    else -> stringResource(buttonMessage)
}

@ThemePreviews
@Composable
private fun ErrorStatePreview() = PreviewSurface {
    ErrorState(
        ErrorAction(R.string.error_no_network, R.string.error_action_network_settings, ButtonAction.NETWORK_SETTINGS),
        onAction = {}
    )
}

@ThemePreviews
@Composable
private fun EmptyStatePreview() = PreviewSurface {
    EmptyState(R.string.error_no_data_bookmark)
}
