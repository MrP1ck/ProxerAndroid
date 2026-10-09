package me.proxer.app.manga

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.proxer.app.util.DeviceUtils

/**
 * @author Ruben Gees
 */
class MangaLinearLayoutManger(
    context: Context,
    readerOrientation: MangaReaderOrientation
) : LinearLayoutManager(
    context,
    if (readerOrientation == MangaReaderOrientation.VERTICAL) VERTICAL else HORIZONTAL,
    readerOrientation == MangaReaderOrientation.RIGHT_TO_LEFT
) {

    private val extraLayoutSpace = when (readerOrientation) {
        // Pages of webtoons are very tall and take a while to decode, so start loading the next one a bit earlier.
        MangaReaderOrientation.VERTICAL -> DeviceUtils.getScreenHeight(context)
        else -> DeviceUtils.getScreenWidth(context) / 2
    }

    init {
        isItemPrefetchEnabled = false
    }

    override fun getExtraLayoutSpace(state: RecyclerView.State) = extraLayoutSpace
}
