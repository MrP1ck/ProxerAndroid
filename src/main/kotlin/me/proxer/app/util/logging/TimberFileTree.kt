package me.proxer.app.util.logging

import android.annotation.SuppressLint
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.util.Log
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.Executors

/**
 * @author Ruben Gees
 */
@SuppressLint("LogNotTimber")
class TimberFileTree : Timber.Tree() {

    private companion object {
        private const val LOGS_DIRECTORY = "Proxer.Me Logs"
        private const val ROTATION_THRESHOLD = 7L

        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val executor = Executors.newSingleThreadExecutor()
    }

    @Suppress("DEPRECATION") // What is Android even doing?
    private val downloadsDirectory
        get() = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)

    private val resolvedLogsDirectory get() = File(downloadsDirectory, LOGS_DIRECTORY).also { it.mkdirs() }

    override fun isLoggable(tag: String?, priority: Int) = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!resolvedLogsDirectory.canWrite()) {
            return
        }

        executor.execute {
            try {
                internalLog(tag, message)
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                Log.e(TimberFileTree::class.java.name, "Failure while logging to file", error)
            }
        }
    }

    private fun internalLog(tag: String?, message: String) {
        val currentLogFiles = resolvedLogsDirectory.listFiles() ?: emptyArray()
        val currentDateTime = LocalDateTime.now()
        val rotationThresholdDate = currentDateTime.toLocalDate().minusDays(ROTATION_THRESHOLD)

        for (currentLogFile in currentLogFiles) {
            val fileDate = try {
                LocalDate.parse(currentLogFile.nameWithoutExtension)
            } catch (error: DateTimeParseException) {
                Log.e(TimberFileTree::class.java.name, "Invalid log file $currentLogFile found, deleting", error)

                null
            }

            if (fileDate == null || fileDate.isBefore(rotationThresholdDate)) {
                currentLogFile.deleteRecursively()
            }
        }

        val logFile = File(resolvedLogsDirectory, "${LocalDate.now()}.log").also { it.createNewFile() }

        val currentDateTimeText = currentDateTime.format(dateTimeFormatter)
        val maybeTag = if (tag != null) "$tag: " else ""

        logFile.appendText("$currentDateTimeText  $maybeTag${message.trim()}\n")
    }
}
