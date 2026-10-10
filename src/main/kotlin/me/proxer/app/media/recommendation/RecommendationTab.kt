package me.proxer.app.media.recommendation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.MediaCoverCard
import me.proxer.app.ui.components.RatingBadge
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.info.Recommendation
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Entries recommended by users who liked this one, with the votes of the community.
 */
@Composable
fun RecommendationTab(
    entryId: String,
    contentPadding: PaddingValues,
    viewModel: RecommendationViewModel = koinViewModel(parameters = { parametersOf(entryId) })
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    ContentStateHost(
        state = state,
        onErrorAction = onErrorAction,
        contentPadding = contentPadding,
        emptyMessage = R.string.error_no_data_recommendations
    ) { recommendations ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(144.dp),
            contentPadding = contentPadding.plus(PaddingValues(16.dp)),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recommendations, key = { it.id }) { recommendation ->
                MediaCoverCard(
                    coverUrl = ProxerUrls.entryImage(recommendation.id),
                    title = recommendation.name,
                    subtitle = recommendation.medium.toAppString(context),
                    onClick = { navigator.openMedia(recommendation.id, recommendation.name, recommendation.category) },
                    overlay = { RecommendationOverlay(recommendation) }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.RecommendationOverlay(recommendation: Recommendation) {
    Box(
        Modifier
            .matchParentSize()
            .padding(8.dp)
    ) {
        if (recommendation.rating > 0) {
            RatingBadge(recommendation.rating, Modifier.align(Alignment.TopStart))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painterResource(
                    when (recommendation.userVote == true) {
                        true -> R.drawable.ic_symbol_thumb_up_filled
                        false -> R.drawable.ic_symbol_thumb_up
                    }
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                recommendation.positiveVotes.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )

            Icon(
                painterResource(
                    when (recommendation.userVote == false) {
                        true -> R.drawable.ic_symbol_thumb_down_filled
                        false -> R.drawable.ic_symbol_thumb_down
                    }
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                recommendation.negativeVotes.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}
