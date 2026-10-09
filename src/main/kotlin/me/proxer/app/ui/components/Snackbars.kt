package me.proxer.app.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The [SnackbarHostState] of the enclosing scaffold. Screens show their snackbars through it instead of creating their
 * own host, so the snackbars are placed correctly above the navigation bar and floating action buttons.
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }
