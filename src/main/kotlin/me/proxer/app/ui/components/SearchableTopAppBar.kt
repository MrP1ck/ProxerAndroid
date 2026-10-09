package me.proxer.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import me.proxer.app.R

/**
 * A top app bar which turns into a search field while [isSearching]. [onQueryChange] is called on every change,
 * [onSearch] when the user submits the query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopAppBar(
    title: String,
    isSearching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    searchHint: String = stringResource(R.string.action_search),
    onSearch: () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearching) { onSearchingChange(false) }

    TopAppBar(
        title = {
            AnimatedContent(
                targetState = isSearching,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SearchableTopAppBar"
            ) { searching ->
                if (searching) {
                    val focusRequester = remember { FocusRequester() }

                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text(searchHint) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onSearch()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    LaunchedEffect(Unit) {
                        if (query.isEmpty()) focusRequester.requestFocus()
                    }
                } else {
                    Text(title)
                }
            }
        },
        navigationIcon = {
            if (isSearching) {
                UpButton(onClick = { onSearchingChange(false) })
            } else {
                navigationIcon()
            }
        },
        actions = {
            if (isSearching) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painterResource(R.drawable.ic_symbol_close),
                            contentDescription = stringResource(R.string.action_clear_search)
                        )
                    }
                }
            } else {
                IconButton(onClick = { onSearchingChange(true) }) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_search),
                        contentDescription = stringResource(R.string.action_search)
                    )
                }

                actions()
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}
