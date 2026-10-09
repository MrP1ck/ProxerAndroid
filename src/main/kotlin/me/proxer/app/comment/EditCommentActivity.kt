package me.proxer.app.comment

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.BBCodeText
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.EmptyState
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.TopAppBarTitle
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.showErrorSnackbar
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Creates or updates the comment of a media entry, with a rating, BBCode formatting and a preview.
 *
 * @author Ruben Gees
 */
class EditCommentActivity : ComposeActivity() {

    companion object {
        const val COMMENT_EXTRA = "comment"

        private const val ID_ARGUMENT = "id"
        private const val ENTRY_ID_ARGUMENT = "entry_id"
        private const val NAME_ARGUMENT = "name_id"
        private const val MAX_LENGTH = 20_000
    }

    private val id: String?
        get() = intent.getStringExtra(ID_ARGUMENT)

    private val entryId: String?
        get() = intent.getStringExtra(ENTRY_ID_ARGUMENT)

    private val name: String?
        get() = intent.getStringExtra(NAME_ARGUMENT)

    private val viewModel by viewModel<EditCommentViewModel> { parametersOf(id, entryId) }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)
        val isUpdate by viewModel.isUpdate.observeAsState(false)
        val publishResult by viewModel.publishResult.observeAsState()
        val publishError by viewModel.publishError.observeAsState()

        var text by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
        var isPreviewVisible by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(state.data) {
            val content = state.data?.content

            if (text.text.isBlank() && !content.isNullOrBlank()) {
                text = TextFieldValue(content, TextRange(content.length))
            }
        }

        LaunchedEffect(publishResult) {
            if (publishResult != null) {
                setResult(Activity.RESULT_OK, Intent().putExtra(COMMENT_EXTRA, publishResult))
                toast(R.string.fragment_edit_comment_published)
                finish()
            }
        }

        ProxerScaffold(
            topBar = {
                TopAppBar(
                    title = {
                        TopAppBarTitle(
                            title = stringResource(
                                if (isUpdate == true) R.string.action_update_comment else R.string.action_create_comment
                            ),
                            subtitle = name?.trim()
                        )
                    },
                    navigationIcon = { UpButton(::finish) },
                    actions = {
                        IconButton(onClick = { isPreviewVisible = true }, enabled = state.data != null) {
                            Icon(
                                painterResource(R.drawable.ic_symbol_visibility),
                                contentDescription = stringResource(R.string.fragment_edit_comment_preview)
                            )
                        }

                        IconButton(onClick = viewModel::publish, enabled = state.data != null && !state.isLoading) {
                            Icon(
                                painterResource(R.drawable.ic_symbol_send),
                                contentDescription = stringResource(R.string.action_publish)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            val snackbarHostState = LocalSnackbarHostState.current

            LaunchedEffect(publishError) {
                publishError?.let {
                    snackbarHostState?.showErrorSnackbar(
                        context = context,
                        error = it,
                        onAction = onErrorAction,
                        message = context.getString(R.string.error_comment_publish, context.getString(it.message))
                    )
                }
            }

            ContentStateHost(
                state = state,
                onErrorAction = onErrorAction,
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) { comment ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .imePadding()
                ) {
                    if (state.isLoading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Rules()

                        RatingInput(
                            rating = comment.overallRating,
                            onRatingChange = { viewModel.updateRating(it / 2f) }
                        )

                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                if (it.text != text.text) viewModel.updateContent(it.text)

                                text = it
                            },
                            placeholder = { Text(stringResource(R.string.fragment_edit_comment_hint)) },
                            supportingText = { Text("${text.text.length} / $MAX_LENGTH") },
                            isError = text.text.length > MAX_LENGTH,
                            minLines = 8,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    FormattingBar(onInsertTag = { tag, value -> text = text.insertTag(tag, value) })
                }

                if (isPreviewVisible) {
                    ModalBottomSheet(onDismissRequest = { isPreviewVisible = false }) {
                        Text(
                            text = stringResource(R.string.fragment_edit_comment_preview),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        if (comment.content.isBlank()) {
                            EmptyState(R.string.fragment_edit_comment_preview_empty, icon = R.drawable.ic_symbol_edit)
                        } else {
                            BBCodeText(
                                tree = comment.parsedContent,
                                expandSpoilers = true,
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        entryId?.also { safeEntryId ->
            val content = viewModel.data.value?.content ?: ""

            if (viewModel.isUpdate.value == false && content.isNotBlank()) {
                storageHelper.putCommentDraft(safeEntryId, content)
                toast(R.string.fragment_edit_comment_draft_saved)
            } else {
                storageHelper.deleteCommentDraft(safeEntryId)
            }
        }

        super.onDestroy()
    }

    @Composable
    private fun Rules() {
        var isExpanded by rememberSaveable { mutableStateOf(false) }
        val rules = stringArrayResource(R.array.fragment_edit_comment_rules)

        OutlinedCard(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_symbol_gavel), contentDescription = null)

                Spacer(Modifier.width(16.dp))

                Text(
                    text = stringResource(R.string.fragment_edit_comment_rules_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_expand_more),
                        contentDescription = stringResource(R.string.fragment_edit_comment_rules_expand_content_description),
                        modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                    )
                }
            }

            AnimatedVisibility(isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rules.forEach { rule ->
                        Row {
                            Text("•", modifier = Modifier.padding(end = 8.dp))
                            Text(remember(rule) { AnnotatedString.fromHtml(rule.trim()) }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    class Contract : ActivityResultContract<Contract.Input, LocalComment?>() {

        override fun createIntent(context: Context, input: Input) = context.intentFor<EditCommentActivity>(
            ID_ARGUMENT to input.id,
            ENTRY_ID_ARGUMENT to input.entryId,
            NAME_ARGUMENT to input.name
        )

        override fun parseResult(resultCode: Int, intent: Intent?): LocalComment? {
            if (resultCode != Activity.RESULT_OK) {
                return null
            }

            return intent?.getParcelableExtra(COMMENT_EXTRA)
        }

        data class Input(val id: String? = null, val entryId: String? = null, val name: String? = null)
    }
}

/**
 * The overall rating from 0 to 10 as five stars, each of which can be half filled by tapping its left half.
 */
@Composable
private fun RatingInput(rating: Int, onRatingChange: (Int) -> Unit) {
    Column {
        Text(stringResource(ratingTitle(rating)), style = MaterialTheme.typography.titleSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                val icon = when {
                    rating >= (index + 1) * 2 -> R.drawable.ic_symbol_star_filled
                    rating == index * 2 + 1 -> R.drawable.ic_symbol_star_half
                    else -> R.drawable.ic_symbol_star
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .pointerInput(index) {
                            detectTapGestures { offset ->
                                onRatingChange(if (offset.x < size.width / 2) index * 2 + 1 else (index + 1) * 2)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = if (icon == R.drawable.ic_symbol_star) MaterialTheme.colorScheme.outline else StarColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (rating > 0) {
                IconButton(onClick = { onRatingChange(0) }) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_close),
                        contentDescription = stringResource(R.string.fragment_edit_comment_rating_clear_content_description)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormattingBar(onInsertTag: (tag: String, value: String) -> Unit) {
    val context = LocalContext.current

    Surface(tonalElevation = 2.dp) {
        Column {
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp)
            ) {
                FormatButton(R.drawable.ic_symbol_format_bold, R.string.fragment_edit_comment_bold) { onInsertTag("b", "") }
                FormatButton(R.drawable.ic_symbol_format_italic, R.string.fragment_edit_comment_italic) { onInsertTag("i", "") }
                FormatButton(R.drawable.ic_symbol_format_underlined, R.string.fragment_edit_comment_underline) {
                    onInsertTag("u", "")
                }
                FormatButton(R.drawable.ic_symbol_format_strikethrough, R.string.fragment_edit_comment_strikethrough) {
                    onInsertTag("s", "")
                }

                FormatMenu(
                    icon = R.drawable.ic_symbol_format_size,
                    description = R.string.fragment_edit_comment_size,
                    entries = listOf(
                        R.string.fragment_edit_comment_size_huge to "5",
                        R.string.fragment_edit_comment_size_large to "4",
                        R.string.fragment_edit_comment_size_normal to "3",
                        R.string.fragment_edit_comment_size_small to "2",
                        R.string.fragment_edit_comment_size_tiny to "1"
                    ),
                    onSelect = { onInsertTag("size", it) }
                )

                FormatMenu(
                    icon = R.drawable.ic_symbol_format_color_fill,
                    description = R.string.fragment_edit_comment_color,
                    entries = listOf(
                        R.string.fragment_edit_comment_color_red to R.color.red_600,
                        R.string.fragment_edit_comment_color_purple to R.color.purple_600,
                        R.string.fragment_edit_comment_color_blue to R.color.blue_600,
                        R.string.fragment_edit_comment_color_green to R.color.green_600,
                        R.string.fragment_edit_comment_color_yellow to R.color.yellow_600,
                        R.string.fragment_edit_comment_color_orange to R.color.orange_600,
                        R.string.fragment_edit_comment_color_grey to R.color.grey_600,
                        R.string.fragment_edit_comment_color_white to android.R.color.white
                    ).map { (title, color) -> title to colorString(context, color) },
                    onSelect = { onInsertTag("color", it) },
                    leadingColor = { Color(android.graphics.Color.parseColor(it)) }
                )

                FormatButton(R.drawable.ic_symbol_format_align_left, R.string.fragment_edit_comment_left) {
                    onInsertTag("left", "")
                }
                FormatButton(R.drawable.ic_symbol_format_align_center, R.string.fragment_edit_comment_center) {
                    onInsertTag("center", "")
                }
                FormatButton(R.drawable.ic_symbol_format_align_right, R.string.fragment_edit_comment_right) {
                    onInsertTag("right", "")
                }
                FormatButton(R.drawable.ic_symbol_visibility_off, R.string.fragment_edit_comment_spoiler) {
                    onInsertTag("spoiler", "")
                }
            }
        }
    }
}

@Composable
private fun FormatButton(@DrawableRes icon: Int, @StringRes description: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = stringResource(description))
    }
}

@Composable
private fun FormatMenu(
    @DrawableRes icon: Int,
    @StringRes description: Int,
    entries: List<Pair<Int, String>>,
    onSelect: (String) -> Unit,
    leadingColor: ((String) -> Color)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        FormatButton(icon, description) { isExpanded = true }

        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            entries.forEach { (title, value) ->
                DropdownMenuItem(
                    text = { Text(stringResource(title)) },
                    leadingIcon = leadingColor?.let {
                        {
                            Surface(shape = CircleShape, color = it(value), modifier = Modifier.size(20.dp)) {}
                        }
                    },
                    onClick = {
                        isExpanded = false

                        onSelect(value)
                    }
                )
            }
        }
    }
}

/**
 * Wraps the selection in the BBCode [tag] (or inserts an empty one at the cursor) and places the cursor before the
 * closing tag.
 */
internal fun TextFieldValue.insertTag(tag: String, value: String = ""): TextFieldValue {
    val startTag = "[$tag${if (value.isNotEmpty()) "=$value" else ""}]"
    val endTag = "[/$tag]"
    val start = selection.min
    val end = selection.max

    val newText = text.substring(0, start) + startTag + text.substring(start, end) + endTag + text.substring(end)

    return TextFieldValue(newText, TextRange(end + startTag.length))
}

private fun colorString(context: Context, color: Int) =
    "#${Integer.toHexString(ContextCompat.getColor(context, color) and 0x00ffffff).padStart(6, '0')}"

private fun ratingTitle(rating: Int) = when (rating) {
    1 -> R.string.fragment_edit_comment_rating_title_1
    2 -> R.string.fragment_edit_comment_rating_title_2
    3 -> R.string.fragment_edit_comment_rating_title_3
    4 -> R.string.fragment_edit_comment_rating_title_4
    5 -> R.string.fragment_edit_comment_rating_title_5
    6 -> R.string.fragment_edit_comment_rating_title_6
    7 -> R.string.fragment_edit_comment_rating_title_7
    8 -> R.string.fragment_edit_comment_rating_title_8
    9 -> R.string.fragment_edit_comment_rating_title_9
    10 -> R.string.fragment_edit_comment_rating_title_10
    else -> R.string.fragment_edit_comment_rating_title_0
}

private val StarColor = Color(0xFFFFC107)
