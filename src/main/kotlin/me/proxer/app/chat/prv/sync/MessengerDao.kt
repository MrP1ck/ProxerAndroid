package me.proxer.app.chat.prv.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import me.proxer.app.auth.LocalUser
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.library.enums.Device
import me.proxer.library.enums.MessageAction
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
@Dao
abstract class MessengerDao {

    @Transaction
    open suspend fun insertMessageToSend(user: LocalUser, text: String, conferenceId: Long): LocalMessage {
        val message = LocalMessage(
            calculateNextMessageToSendId(),
            conferenceId,
            user.id,
            user.name,
            text,
            MessageAction.NONE,
            Instant.now(),
            Device.MOBILE
        )

        insertMessage(message)
        markConferenceAsRead(conferenceId)

        return message
    }

    @Transaction
    open suspend fun clear() {
        clearMessages()
        clearConferences()
    }

    /**
     * The messages of the conference: the unsent ones (with a negative id) followed by the sent ones.
     */
    open fun getMessagesFlowForConference(conferenceId: Long): Flow<List<LocalMessage>> {
        val sentFlow = getSentMessagesFlowForConference(conferenceId).map { MessageSource.Sent(it) }
        val unsentFlow = getUnsentMessagesFlowForConference(conferenceId).map { MessageSource.Unsent(it) }

        return flow {
            var sent = emptyList<LocalMessage>()
            var unsent = emptyList<LocalMessage>()
            var current: List<LocalMessage>? = null

            merge(sentFlow, unsentFlow).collect { source ->
                when (source) {
                    is MessageSource.Sent -> {
                        sent = source.messages
                        current = unsent + sent

                        emit(unsent + sent)
                    }
                    is MessageSource.Unsent -> {
                        unsent = source.messages

                        // Prevent duplicate update when detecting sent messages.
                        // If the amount of unsent messages is lower than before, these messages have been sent.
                        // Without this check, both sources would update (racy) and potentially cause weird visual
                        // effects.
                        if (unsent.size >= (current?.takeWhile { message -> message.id < 0 }?.size ?: 0)) {
                            current = unsent + sent

                            emit(unsent + sent)
                        }
                    }
                }
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertConferences(conference: List<LocalConference>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessages(messages: List<LocalMessage>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertMessage(message: LocalMessage): Long

    @Query("SELECT * FROM conferences ORDER BY date DESC")
    abstract suspend fun getConferences(): List<LocalConference>

    @Query("SELECT * FROM conferences ORDER BY date DESC LIMIT :amount")
    abstract suspend fun getMostRecentConferences(amount: Int): List<LocalConference>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
            SELECT * FROM conferences
            LEFT JOIN (
                SELECT id AS messageId, conferenceId, userId, message AS messageText, username,
                       `action` as messageAction from messages
                GROUP BY conferenceId
                HAVING MAX(date)
                ORDER BY date DESC, id
            ) AS messages
            ON conferences.id = messages.conferenceId
            WHERE topic LIKE '%' || :searchQuery || '%'
            ORDER BY date DESC
            """
    )
    abstract fun getConferencesFlow(searchQuery: String): Flow<List<ConferenceWithMessage>>

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    abstract fun getConferenceFlow(id: Long): Flow<LocalConference?>

    @Query("SELECT * FROM conferences WHERE localIsRead = 0 AND isRead = 0 ORDER BY id DESC")
    abstract suspend fun getUnreadConferences(): List<LocalConference>

    @Query("SELECT * FROM conferences WHERE localIsRead != 0 AND isRead = 0")
    abstract suspend fun getConferencesToMarkAsRead(): List<LocalConference>

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    abstract suspend fun getConference(id: Long): LocalConference

    @Query("SELECT * FROM conferences WHERE id = :id LIMIT 1")
    abstract suspend fun findConference(id: Long): LocalConference?

    @Query("SELECT * FROM conferences WHERE topic = :username LIMIT 1")
    abstract suspend fun findConferenceForUser(username: String): LocalConference?

    @Query("SELECT * FROM (SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC) ")
    abstract fun getSentMessagesFlowForConference(conferenceId: Long): Flow<List<LocalMessage>>

    @Query("SELECT * FROM (SELECT * FROM messages WHERE conferenceId = :conferenceId AND id < 0 ORDER BY id ASC)")
    abstract fun getUnsentMessagesFlowForConference(conferenceId: Long): Flow<List<LocalMessage>>

    @Query("SELECT COUNT(*) FROM messages WHERE conferenceId = :conferenceId AND id = :lastReadMessageId")
    abstract suspend fun getUnreadMessageAmountForConference(conferenceId: Long, lastReadMessageId: Long): Int

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC LIMIT :amount")
    abstract suspend fun getMostRecentMessagesForConference(conferenceId: Long, amount: Int): List<LocalMessage>

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id DESC LIMIT 1")
    abstract suspend fun findMostRecentMessageForConference(conferenceId: Long): LocalMessage?

    @Query("SELECT * FROM messages WHERE conferenceId = :conferenceId AND id >= 0 ORDER BY id ASC LIMIT 1")
    abstract suspend fun findOldestMessageForConference(conferenceId: Long): LocalMessage?

    @Query("SELECT MIN(id) FROM messages")
    abstract suspend fun findLowestMessageId(): Long?

    @Query("SELECT * FROM messages WHERE id < 0 ORDER BY id DESC")
    abstract suspend fun getMessagesToSend(): List<LocalMessage>

    @Query("DELETE FROM messages WHERE id = :messageId")
    abstract suspend fun deleteMessageToSend(messageId: Long)

    @Query("UPDATE conferences SET localIsRead = 1 WHERE id = :conferenceId")
    abstract suspend fun markConferenceAsRead(conferenceId: Long)

    @Query("UPDATE conferences SET isFullyLoaded = 1 WHERE id = :conferenceId")
    abstract suspend fun markConferenceAsFullyLoaded(conferenceId: Long)

    @Query("DELETE FROM messages WHERE conferenceId = :conferenceId")
    abstract suspend fun deleteMessagesByConferenceId(conferenceId: String)

    @Query("DELETE FROM conferences WHERE id = :id")
    abstract suspend fun deleteConferenceById(id: String)

    @Query("DELETE FROM conferences")
    abstract suspend fun clearConferences()

    @Query("DELETE FROM messages")
    abstract suspend fun clearMessages()

    private suspend fun calculateNextMessageToSendId(): Long {
        val candidate = findLowestMessageId() ?: -1L

        return when (candidate < 0) {
            true -> candidate - 1
            false -> -1L
        }
    }

    private sealed class MessageSource {
        class Sent(val messages: List<LocalMessage>) : MessageSource()
        class Unsent(val messages: List<LocalMessage>) : MessageSource()
    }
}
