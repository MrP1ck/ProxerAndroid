package me.proxer.app.chat.prv.message

import androidx.room.withTransaction
import me.proxer.app.chat.ReportViewModel
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerDatabase
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerApi

class MessengerReportViewModel : ReportViewModel() {

    private val api by safeInject<ProxerApi>()
    private val messengerDao by safeInject<MessengerDao>()
    private val messengerDatabase by safeInject<MessengerDatabase>()

    override suspend fun report(id: String, message: String) {
        api.messenger.report(id, message).awaitNullable()

        messengerDatabase.withTransaction {
            val conference = messengerDao.getConference(id.toLong())

            if (!conference.isGroup) {
                messengerDao.deleteMessagesByConferenceId(id)
                messengerDao.deleteConferenceById(id)
            }
        }
    }
}
