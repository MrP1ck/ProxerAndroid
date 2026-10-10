package me.proxer.app.media.list

import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.media.LocalTag
import me.proxer.app.media.TagDao
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.enumSetOf
import me.proxer.app.util.extension.isAgeRestricted
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalDate
import me.proxer.app.util.extension.toParcelableTag
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.entity.list.Tag
import me.proxer.library.enums.MediaType
import me.proxer.library.enums.TagRateFilter
import me.proxer.library.enums.TagSpoilerFilter
import me.proxer.library.enums.TagType
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

/**
 * The anime or manga search. The [filter] is applied with [updateFilter].
 *
 * @author Ruben Gees
 */
class MediaListViewModel(initialFilter: MediaListFilter) : PagedContentViewModel<MediaListEntry>() {

    override val itemsOnPage = 30

    override val isLoginRequired: Boolean
        get() = super.isLoginRequired || filter.value.type.isAgeRestricted()

    override val isAgeConfirmationRequired: Boolean
        get() = isLoginRequired

    override val endpoint: PagingLimitEndpoint<List<MediaListEntry>>
        get() = filter.value.let { filter ->
            api.list.mediaSearch()
                .sort(filter.sortCriteria)
                .name(filter.searchQuery?.takeIf { it.isNotBlank() })
                .language(filter.language)
                .genres(filter.genres.asSequence().map { it.id }.toSet())
                .excludedGenres(filter.excludedGenres.asSequence().map { it.id }.toSet())
                .fskConstraints(enumSetOf(filter.fskConstraints))
                .tags(filter.tags.asSequence().map { it.id }.toSet())
                .excludedTags(filter.excludedTags.asSequence().map { it.id }.toSet())
                .tagRateFilter(if (filter.includeUnratedTags) TagRateFilter.ALL else TagRateFilter.RATED_ONLY)
                .tagSpoilerFilter(if (filter.includeSpoilerTags) TagSpoilerFilter.ALL else TagSpoilerFilter.NO_SPOILERS)
                .hideFinished(filter.hideFinished)
                .type(filter.type)
        }

    private val mutableFilter = MutableStateFlow(initialFilter)

    /** The current filter. Changes of the type and the sort criteria are applied immediately. */
    val filter: StateFlow<MediaListFilter> = mutableFilter.asStateFlow()

    /**
     * Applies the [newFilter] and reloads the data if it changed.
     */
    fun updateFilter(newFilter: MediaListFilter) {
        val oldFilter = mutableFilter.value

        if (oldFilter != newFilter) {
            mutableFilter.value = newFilter

            reload()

            if (oldFilter.type.isAgeRestricted() != newFilter.type.isAgeRestricted()) {
                loadTags()
            }
        }
    }

    val genreData = MutableLiveData<List<LocalTag>>()
    val tagData = MutableLiveData<List<LocalTag>>()

    private val tagDao by safeInject<TagDao>()

    private var tagsDisposable: Disposable? = null

    override fun onCleared() {
        tagsDisposable?.dispose()
        tagsDisposable = null

        super.onCleared()
    }

    override fun areItemsTheSame(old: MediaListEntry, new: MediaListEntry) = old == new

    fun loadTags() {
        tagsDisposable?.dispose()
        tagsDisposable = Single
            .fromCallable { tagDao.getTags() }
            .flatMap { cachedTags ->
                when {
                    shouldUpdateTags() || cachedTags.isEmpty() ->
                        tagSingle()
                            .map { remoteTags -> remoteTags.map { it.toParcelableTag() } }
                            .doOnSuccess {
                                tagDao.replaceTags(it)

                                preferenceHelper.lastTagUpdateDate = Instant.now()
                            }
                    else -> Single.just(cachedTags)
                }
            }
            .map {
                val tagsToFilter = when (filter.value.type) {
                    MediaType.HENTAI, MediaType.HMANGA -> enumSetOf(TagType.TAG, TagType.H_TAG)
                    else -> enumSetOf(TagType.TAG)
                }

                val genreTags = it.filter { tag -> tag.type == TagType.GENRE }
                val entryTags = it.filter { tag -> tag.type in tagsToFilter }

                TagContainer(genreTags, entryTags)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors { tags ->
                genreData.value = tags.genreTags
                tagData.value = tags.entryTags
            }
    }

    private fun tagSingle(): Single<List<Tag>> {
        return Single.zip(
            api.list.tagList().buildSingle(),
            api.list.tagList().type(TagType.H_TAG).buildSingle(),
            { first: List<Tag>, second: List<Tag> -> first + second }
        )
    }

    private fun shouldUpdateTags() = preferenceHelper.lastTagUpdateDate.toLocalDate()
        .isBefore(LocalDate.now().minusDays(15))

    private data class TagContainer(val genreTags: List<LocalTag>, val entryTags: List<LocalTag>)
}
