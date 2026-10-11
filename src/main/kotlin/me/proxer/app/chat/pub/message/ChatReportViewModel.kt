package me.proxer.app.chat.pub.message

import me.proxer.app.chat.ReportViewModel
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerApi

class ChatReportViewModel : ReportViewModel() {

    private val api by safeInject<ProxerApi>()

    override suspend fun report(id: String, message: String) {
        api.chat.reportMessage(id, message).awaitNullable()
    }
}
