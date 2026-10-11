package me.proxer.app.base

import me.proxer.app.util.Validators
import me.proxer.app.util.data.EventBus
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.ProxerApi

/**
 * What nearly all ViewModels need. Passed to [BaseViewModel], so the ViewModels do not have to repeat it.
 */
class ViewModelDependencies(
    val api: ProxerApi,
    val storageHelper: StorageHelper,
    val preferenceHelper: PreferenceHelper,
    val validators: Validators,
    val bus: EventBus
)
