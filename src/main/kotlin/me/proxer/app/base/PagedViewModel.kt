package me.proxer.app.base

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.library.entity.ProxerIdItem
import timber.log.Timber

/**
 * @author Ruben Gees
 */
abstract class PagedViewModel<T>(dependencies: ViewModelDependencies) : BaseViewModel<List<T>>(dependencies) {

    val refreshError = Events<ErrorUtils.ErrorAction>()

    protected open var hasReachedEnd = false
    protected var isRefreshing = false

    protected var page = 0
        get() = if (isRefreshing) 0 else field

    protected abstract val itemsOnPage: Int

    override fun load() {
        val currentPage = page

        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            error.value = null
            isLoading.value = true

            try {
                val newData = loadData()

                hasReachedEnd = newData.size < itemsOnPage

                val mergedData = mergeNewDataWithExistingData(data.value ?: emptyList(), newData, currentPage)

                if (!isRefreshing) page++
                isRefreshing = false

                error.value = null
                data.value = mergedData
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") loadError: Throwable) {
                Timber.e(loadError)

                isRefreshing = false

                if (currentPage == 0 && (data.value?.size ?: 0) > 0) {
                    refreshError.send(ErrorUtils.handle(loadError))
                } else {
                    error.value = ErrorUtils.handle(loadError)
                }
            } finally {
                if (isActive) isLoading.value = false
            }
        }
    }

    override fun loadIfPossible() {
        if (!hasReachedEnd) {
            super.loadIfPossible()
        }
    }

    override fun refresh() {
        isRefreshing = true

        load()
    }

    override fun reload() {
        hasReachedEnd = false
        isRefreshing = false
        page = 0

        super.reload()
    }

    protected open fun areItemsTheSame(old: T, new: T) = when {
        old is ProxerIdItem && new is ProxerIdItem -> old.id == new.id
        else -> old == new
    }

    protected open fun mergeNewDataWithExistingData(
        existingData: List<T>,
        newData: List<T>,
        currentPage: Int
    ) = when (currentPage) {
        0 -> newData + existingData.filter { oldItem ->
            newData.find { newItem -> areItemsTheSame(oldItem, newItem) } == null
        }
        else -> existingData.filter { oldItem ->
            newData.find { newItem -> areItemsTheSame(oldItem, newItem) } == null
        } + newData
    }
}
