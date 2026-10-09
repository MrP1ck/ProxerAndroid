package me.proxer.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import me.proxer.app.R
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.compose.BBCodeContent
import me.proxer.app.ui.view.bbcode.compose.BBCodeEnvironment
import me.proxer.app.ui.view.bbcode.compose.LocalBBCodeEnvironment
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import org.koin.core.context.GlobalContext

/**
 * Renders parsed BBCode (posts, comments, messages).
 *
 * If [collapsedHeight] is set, content taller than it is cut off and can be expanded with a button.
 */
@Composable
fun BBCodeText(
    tree: BBTree,
    modifier: Modifier = Modifier,
    userId: String? = null,
    enableEmoticons: Boolean = false,
    collapsedHeight: Dp? = null,
    key: String? = null
) {
    val environment = rememberBBCodeEnvironment()
    val collapsedHeightPx = with(LocalDensity.current) { collapsedHeight?.roundToPx() }

    var isExpanded by rememberSaveable(key) { mutableStateOf(false) }
    var isOverflowing by rememberSaveable(key) { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalBBCodeEnvironment provides environment,
        LocalTextStyle provides MaterialTheme.typography.bodyMedium
    ) {
        Column(modifier) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .let {
                        if (collapsedHeight != null && !isExpanded) it.heightIn(max = collapsedHeight).clipToBounds() else it
                    }
            ) {
                BBCodeContent(
                    tree = tree,
                    userId = userId,
                    enableEmoticons = enableEmoticons,
                    modifier = Modifier.onSizeChanged { size ->
                        if (collapsedHeightPx != null && !isExpanded) {
                            isOverflowing = size.height > collapsedHeightPx
                        }
                    }
                )
            }

            if (collapsedHeight != null && (isOverflowing || isExpanded)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_symbol_expand_more),
                            contentDescription = null,
                            modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The environment for BBCode in the app: the login and age confirmation state and the navigation to links, images and
 * profiles. Without Koin (e.g. in previews), the content is shown as for a guest.
 */
@Composable
private fun rememberBBCodeEnvironment(): BBCodeEnvironment {
    val context = LocalContext.current
    val koin = GlobalContext.getOrNull()

    return remember(context, koin) {
        val activity = BBUtils.findBaseActivity(context)

        BBCodeEnvironment(
            isLoggedIn = koin?.getOrNull<StorageHelper>()?.isLoggedIn == true,
            isAgeRestrictedMediaAllowed = koin?.getOrNull<PreferenceHelper>()?.isAgeRestrictedMediaAllowed == true,
            onUrl = { url -> activity?.showPage(url, forceBrowser = false, skipCheck = false) },
            onMention = { username -> activity?.let { ProfileActivity.navigateTo(it, null, username) } },
            onImage = { url -> activity?.let { ImageDetailActivity.navigateTo(it, url) } }
        )
    }
}
