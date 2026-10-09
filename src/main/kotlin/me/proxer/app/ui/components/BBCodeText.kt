package me.proxer.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree

/**
 * Renders parsed BBCode (posts, comments, profile texts).
 *
 * This hosts the View based BBCodeView for now. Screens use this composable, so the renderer can be replaced without
 * touching them.
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
    val textColor = LocalContentColor.current.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val collapsedHeightPx = with(LocalDensity.current) { collapsedHeight?.roundToPx() }

    var isExpanded by rememberSaveable(key) { mutableStateOf(false) }
    var isOverflowing by rememberSaveable(key) { mutableStateOf(false) }

    Column(modifier) {
        AndroidView(
            factory = { context ->
                BBCodeView(context).apply { glide = Glide.with(context) }
            },
            update = { view ->
                view.textColor = textColor
                view.spoilerTextColor = linkColor
                view.userId = userId
                view.enableEmotions = enableEmoticons
                view.maxHeight = if (isExpanded || collapsedHeightPx == null) Int.MAX_VALUE else collapsedHeightPx

                if (view.tree != tree) view.tree = tree
            },
            onReset = { view -> view.tree = null },
            onRelease = { view -> view.destroyWithRetainingViews() },
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .onSizeChanged { size ->
                    if (!isExpanded && collapsedHeightPx != null) {
                        isOverflowing = size.height >= collapsedHeightPx
                    }
                }
        )

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
