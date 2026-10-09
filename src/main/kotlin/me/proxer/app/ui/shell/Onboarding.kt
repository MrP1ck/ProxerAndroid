package me.proxer.app.ui.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.settings.theme.Theme
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.ThemeVariant
import me.proxer.app.ui.components.ThemePicker
import me.proxer.app.ui.preview.PreviewSurface

private const val PAGE_COUNT = 3

/**
 * The introduction shown on the first launch: a welcome, the notification opt-in and the theme selection.
 * [onFinish] is called with whether notifications should be enabled.
 */
@Composable
fun Onboarding(
    themeContainer: ThemeContainer,
    onThemeContainerChange: (ThemeContainer) -> Unit,
    onFinish: (areNotificationsEnabled: Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    var areNotificationsEnabled by rememberSaveable { mutableStateOf(true) }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        icon = R.drawable.ic_proxer,
                        title = R.string.introduction_welcome_title,
                        description = R.string.introduction_welcome_description,
                        isLogo = true
                    )
                    1 -> OnboardingPage(
                        icon = R.drawable.ic_symbol_notifications,
                        title = R.string.introduction_notifications_title,
                        description = R.string.introduction_notifications_description
                    ) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.introduction_notifications_switch)) },
                            trailingContent = {
                                Switch(
                                    checked = areNotificationsEnabled,
                                    onCheckedChange = { areNotificationsEnabled = it }
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.clip(MaterialTheme.shapes.large)
                        )
                    }
                    else -> OnboardingPage(
                        icon = R.drawable.ic_symbol_palette,
                        title = R.string.introduction_design_title,
                        description = R.string.introduction_design_description
                    ) {
                        ThemePicker(themeContainer, onThemeContainerChange)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onFinish(areNotificationsEnabled) }) {
                    Text(stringResource(R.string.introduction_skip))
                }

                PageIndicator(
                    current = pagerState.currentPage,
                    count = PAGE_COUNT,
                    modifier = Modifier.weight(1f)
                )

                val isLastPage = pagerState.currentPage == PAGE_COUNT - 1

                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish(areNotificationsEnabled)
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
                ) {
                    Text(stringResource(if (isLastPage) R.string.introduction_done else R.string.introduction_next))
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes description: Int,
    isLogo: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLogo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (isLogo) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.size(if (isLogo) 80.dp else 56.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(description).trim(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            content()
        }
    }
}

@Composable
private fun PageIndicator(current: Int, count: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingPreview() = PreviewSurface {
    Onboarding(ThemeContainer(Theme.CLASSIC, ThemeVariant.SYSTEM), onThemeContainerChange = {}, onFinish = {})
}
