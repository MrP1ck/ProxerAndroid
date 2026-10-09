package me.proxer.app.ui.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.auth.LocalUser
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.library.util.ProxerUrls

/**
 * Opens the account sheet. Provided by [ProxerApp] for the top level screens.
 */
val LocalOpenAccountSheet = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * The avatar of the current user in the top app bar of the top level screens. Opens the account sheet.
 */
@Composable
fun AccountButton() {
    val user by rememberCurrentUser()
    val openAccountSheet = LocalOpenAccountSheet.current

    IconButton(onClick = openAccountSheet) {
        UserAvatar(user, Modifier.size(32.dp))
    }
}

@Composable
private fun UserAvatar(user: LocalUser?, modifier: Modifier = Modifier) {
    if (user == null || user.image.isBlank()) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_account_circle),
            contentDescription = stringResource(R.string.section_profile_info),
            modifier = modifier
        )
    } else {
        ProxerAsyncImage(
            url = ProxerUrls.userImage(user.image),
            contentDescription = stringResource(R.string.section_profile_info),
            modifier = modifier.clip(CircleShape),
            showErrorIcon = false
        )
    }
}

/**
 * The account of the user and the secondary destinations of the app: profile, notifications, settings and info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(onDismiss: () -> Unit) {
    val navigator = LocalAppNavigator.current
    val user by rememberCurrentUser()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissAnd(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding()) {
            AccountHeader(
                user = user,
                onLoginClick = { dismissAnd(navigator::showLogin) },
                onProfileClick = { user?.let { dismissAnd { navigator.openProfile(it.id, it.name, it.image) } } }
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            if (user != null) {
                SheetItem(R.drawable.ic_symbol_notifications, R.string.section_notifications) {
                    dismissAnd(navigator::openNotifications)
                }

                SheetItem(R.drawable.ic_symbol_manage_accounts, R.string.section_profile_settings) {
                    dismissAnd(navigator::openProfileSettings)
                }
            }

            SheetItem(R.drawable.ic_symbol_settings, R.string.section_settings) { dismissAnd(navigator::openSettings) }
            SheetItem(R.drawable.ic_symbol_info, R.string.section_info) { dismissAnd(navigator::openAbout) }

            if (user != null) {
                SheetItem(R.drawable.ic_symbol_logout, R.string.section_logout) { dismissAnd(navigator::showLogout) }
            }

            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AccountHeader(user: LocalUser?, onLoginClick: () -> Unit, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user, Modifier.size(56.dp))

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = user?.name ?: stringResource(R.string.section_guest),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(if (user != null) R.string.section_user_subtitle else R.string.error_login),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (user == null) {
            FilledTonalButton(onClick = onLoginClick) { Text(stringResource(R.string.section_login)) }
        } else {
            FilledTonalButton(onClick = onProfileClick) { Text(stringResource(R.string.section_profile_info)) }
        }
    }
}

@Composable
private fun SheetItem(@DrawableRes icon: Int, @StringRes text: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(text)) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    )
}
