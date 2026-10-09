package me.proxer.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.util.extension.flagDrawableRes
import me.proxer.library.enums.Language
import java.util.Locale

/**
 * The average rating of an entry on the scale of 10, on top of a cover image.
 */
@Composable
fun RatingBadge(rating: Float, modifier: Modifier = Modifier) {
    val text = String.format(Locale.GERMANY, "%.1f", rating)

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_star_filled),
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(14.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
fun LanguageFlag(language: Language, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(language.flagDrawableRes),
        contentDescription = null,
        modifier = modifier.size(width = 22.dp, height = 16.dp)
    )
}
