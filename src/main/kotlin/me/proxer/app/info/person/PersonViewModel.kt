package me.proxer.app.info.person

import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Person

class PersonViewModel(
    dependencies: ViewModelDependencies,
    private val personId: String
) : BaseContentViewModel<Person>(dependencies) {

    override val endpoint: Endpoint<Person>
        get() = api.info.person(personId)
}
