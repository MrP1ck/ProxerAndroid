package me.proxer.app.base

import me.proxer.app.util.extension.await
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
abstract class BaseContentViewModel<T : Any>(dependencies: ViewModelDependencies) : BaseViewModel<T>(dependencies) {

    override suspend fun loadData(): T {
        validate()

        return endpoint.await()
    }

    protected abstract val endpoint: Endpoint<T>
}
