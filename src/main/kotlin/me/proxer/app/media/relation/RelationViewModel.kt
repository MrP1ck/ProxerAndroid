package me.proxer.app.media.relation

import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Relation

/**
 * @author Ruben Gees
 */
class RelationViewModel(private val entryId: String) : BaseContentViewModel<List<Relation>>() {

    override val endpoint: Endpoint<List<Relation>>
        get() = api.info.relations(entryId)
            .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)

    override suspend fun loadData() = super.loadData().filterNot { it.id == entryId }
}
