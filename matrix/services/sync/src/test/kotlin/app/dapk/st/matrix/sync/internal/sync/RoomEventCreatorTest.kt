package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.asString
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.request.ApiEncryptedContent
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent
import app.dapk.st.matrix.sync.internal.sync.message.RichMessageParser
import fake.FakeErrorTracker
import fake.FakeRoomMembersService
import fixture.*
import internalfixture.*
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_ROOM_ID = aRoomId()
private val A_SENDER = aRoomMember()
private val EMPTY_LOOKUP = FakeLookup(LookupResult(apiTimelineEvent = null, roomEvent = null))
private val A_TEXT_EVENT_MESSAGE = RichText.of("a text message")
private val A_REPLY_EVENT_MESSAGE = RichText.of("a reply to another message")
private val A_TEXT_EVENT = anApiTimelineTextEvent(
    senderId = A_SENDER.id,
    content = aTimelineTextEventContent(body = A_TEXT_EVENT_MESSAGE.asString())
)
private val A_TEXT_EVENT_WITHOUT_CONTENT = anApiTimelineTextEvent(
    senderId = A_SENDER.id,
    content = aTimelineTextEventContent(body = null)
)
private val A_USER_CREDENTIALS = aUserCredentials()

internal class RoomEventCreatorTest {

    private val fakeRoomMembersService = FakeRoomMembersService()

    private val richMessageParser = RichMessageParser()
    private val roomEventCreator = RoomEventCreator(
        fakeRoomMembersService, FakeErrorTracker(), RoomEventFactory(fakeRoomMembersService, richMessageParser),
        richMessageParser
    )

    @Test
    fun `given Megolm encrypted event then maps to encrypted room message`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val megolmEvent = anEncryptedApiTimelineEvent(senderId = A_SENDER.id, encryptedContent = aMegolmApiEncryptedContent())

        val result = with(roomEventCreator) { megolmEvent.toRoomEvent(A_ROOM_ID) }

        result shouldBeEqualTo anEncryptedRoomMessageEvent(
            eventId = megolmEvent.eventId,
            utcTimestamp = megolmEvent.utcTimestamp,
            author = A_SENDER,
            encryptedContent = megolmEvent.encryptedContent.toMegolm(),
        )
    }

    @Test
    fun `given Olm encrypted event then maps to null`() = runTest {
        val olmEvent = anEncryptedApiTimelineEvent(encryptedContent = anOlmApiEncryptedContent())

        val result = with(roomEventCreator) { olmEvent.toRoomEvent(A_ROOM_ID) }

        result shouldBeEqualTo null
    }

    @Test
    fun `given unknown encrypted event then maps to null`() = runTest {
        val olmEvent = anEncryptedApiTimelineEvent(encryptedContent = anUnknownApiEncryptedContent())

        val result = with(roomEventCreator) { olmEvent.toRoomEvent(A_ROOM_ID) }

        result shouldBeEqualTo null
    }

    @Test
    fun `given text event then maps to room message`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)

        val result = with(roomEventCreator) { A_TEXT_EVENT.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, EMPTY_LOOKUP) }

        result shouldBeEqualTo aMatrixRoomMessageEvent(
            eventId = A_TEXT_EVENT.id,
            utcTimestamp = A_TEXT_EVENT.utcTimestamp,
            content = A_TEXT_EVENT_MESSAGE,
            author = A_SENDER,
        )
    }

    @Test
    fun `given text event without body then maps to empty room message`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)

        val result = with(roomEventCreator) { A_TEXT_EVENT_WITHOUT_CONTENT.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, EMPTY_LOOKUP) }

        result shouldBeEqualTo aMatrixRoomMessageEvent(
            eventId = A_TEXT_EVENT_WITHOUT_CONTENT.id,
            utcTimestamp = A_TEXT_EVENT_WITHOUT_CONTENT.utcTimestamp,
            content = RichText(emptyList()),
            author = A_SENDER,
        )
    }

    @Test
    fun `given edited event with no relation then maps to new room message`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val editEvent = anApiTimelineTextEvent().toEditEvent(newTimestamp = 0, messageContent = A_TEXT_EVENT_MESSAGE.asString())

        val result = with(roomEventCreator) { editEvent.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, EMPTY_LOOKUP) }

        result shouldBeEqualTo aMatrixRoomMessageEvent(
            eventId = editEvent.id,
            utcTimestamp = editEvent.utcTimestamp,
            content = RichText.of(editEvent.asTextContent().body!!.trimStart()),
            author = A_SENDER,
            edited = true
        )
    }

    @Test
    fun `given edited event which relates to a timeline event then updates existing message`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val originalMessage = anApiTimelineTextEvent(utcTimestamp = 0)
        val editedMessage = originalMessage.toEditEvent(newTimestamp = 1000, messageContent = A_TEXT_EVENT_MESSAGE.asString())
        val lookup = givenLookup(originalMessage)

        val result = with(roomEventCreator) { editedMessage.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, lookup) }

        result shouldBeEqualTo aMatrixRoomMessageEvent(
            eventId = originalMessage.id,
            utcTimestamp = editedMessage.utcTimestamp,
            content = A_TEXT_EVENT_MESSAGE,
            author = A_SENDER,
            edited = true
        )
    }

    @Test
    fun `given edited event which relates to a room event then updates existing message`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val originalMessage = aMatrixRoomMessageEvent()
        val editedMessage = originalMessage.toEditEvent(newTimestamp = 1000, messageContent = A_TEXT_EVENT_MESSAGE.asString())
        val lookup = givenLookup(originalMessage)

        val result = with(roomEventCreator) { editedMessage.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, lookup) }

        result shouldBeEqualTo aMatrixRoomMessageEvent(
            eventId = originalMessage.eventId,
            utcTimestamp = originalMessage.utcTimestamp,
            content = A_TEXT_EVENT_MESSAGE,
            author = A_SENDER,
            edited = true
        )
    }

    @Test
    fun `given edited event which relates to a room reply event then only updates message`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val originalMessage = aRoomReplyMessageEvent(message = aMatrixRoomMessageEvent())
        val editedMessage = (originalMessage.message as RoomEvent.Message).toEditEvent(newTimestamp = 1000, messageContent = A_TEXT_EVENT_MESSAGE.asString())
        val lookup = givenLookup(originalMessage)

        val result = with(roomEventCreator) { editedMessage.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, lookup) }

        result shouldBeEqualTo aRoomReplyMessageEvent(
            replyingTo = originalMessage.replyingTo,
            message = aMatrixRoomMessageEvent(
                eventId = originalMessage.eventId,
                utcTimestamp = originalMessage.utcTimestamp,
                content = A_TEXT_EVENT_MESSAGE,
                author = A_SENDER,
                edited = true
            ),
        )
    }

    @Test
    fun `given edited event is older than related known timeline event then ignores edit`() = runTest {
        val originalMessage = anApiTimelineTextEvent(utcTimestamp = 1000)
        val editedMessage = originalMessage.toEditEvent(newTimestamp = 0, messageContent = A_TEXT_EVENT_MESSAGE.asString())
        val lookup = givenLookup(originalMessage)

        val result = with(roomEventCreator) { editedMessage.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, lookup) }

        result shouldBeEqualTo null
    }

    @Test
    fun `given edited event is older than related room event then ignores edit`() = runTest {
        val originalMessage = aMatrixRoomMessageEvent(utcTimestamp = 1000)
        val editedMessage = originalMessage.toEditEvent(newTimestamp = 0, messageContent = A_TEXT_EVENT_MESSAGE.asString())
        val lookup = givenLookup(originalMessage)

        val result = with(roomEventCreator) { editedMessage.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, lookup) }

        result shouldBeEqualTo null
    }

    @Test
    fun `given reply event with no relation then maps to new room message using the full body`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val replyEvent = anApiTimelineTextEvent().toReplyEvent(messageContent = A_TEXT_EVENT_MESSAGE.asString())

        println(replyEvent.content)
        val result = with(roomEventCreator) { replyEvent.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, EMPTY_LOOKUP) }

        result shouldBeEqualTo aMatrixRoomMessageEvent(
            eventId = replyEvent.id,
            utcTimestamp = replyEvent.utcTimestamp,
            content = RichText.of(replyEvent.asTextContent().body!!),
            author = A_SENDER,
        )
    }

    @Test
    fun `given reply event which relates to a timeline event then maps to reply`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val originalMessage = anApiTimelineTextEvent(content = aTimelineTextEventContent(body = "message being replied to"))
        val replyMessage = originalMessage.toReplyEvent(messageContent = A_REPLY_EVENT_MESSAGE.asString())
        val lookup = givenLookup(originalMessage)

        val result = with(roomEventCreator) { replyMessage.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, lookup) }

        result shouldBeEqualTo aRoomReplyMessageEvent(
            replyingTo = aMatrixRoomMessageEvent(
                eventId = originalMessage.id,
                utcTimestamp = originalMessage.utcTimestamp,
                content = RichText.of(originalMessage.asTextContent().body!!),
                author = A_SENDER,
            ),
            message = aMatrixRoomMessageEvent(
                eventId = replyMessage.id,
                utcTimestamp = replyMessage.utcTimestamp,
                content = A_REPLY_EVENT_MESSAGE,
                author = A_SENDER,
            ),
        )
    }

    @Test
    fun `given reply event which relates to a room event then maps to reply`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val originalMessage = aMatrixRoomMessageEvent()
        val replyMessage = originalMessage.toReplyEvent(messageContent = A_REPLY_EVENT_MESSAGE.asString())
        val lookup = givenLookup(originalMessage)

        val result = with(roomEventCreator) { replyMessage.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, lookup) }

        result shouldBeEqualTo aRoomReplyMessageEvent(
            replyingTo = originalMessage,
            message = aMatrixRoomMessageEvent(
                eventId = replyMessage.id,
                utcTimestamp = replyMessage.utcTimestamp,
                content = A_REPLY_EVENT_MESSAGE,
                author = A_SENDER,
            ),
        )
    }

    @Test
    fun `given reply event which relates to another room reply event then maps to reply with the reply's message`() = runTest {
        fakeRoomMembersService.givenMember(A_ROOM_ID, A_SENDER.id, A_SENDER)
        val originalMessage = aRoomReplyMessageEvent()
        val replyMessage = (originalMessage.message as RoomEvent.Message).toReplyEvent(messageContent = A_REPLY_EVENT_MESSAGE.asString())
        val lookup = givenLookup(originalMessage)

        val result = with(roomEventCreator) { replyMessage.toRoomEvent(A_USER_CREDENTIALS, A_ROOM_ID, lookup) }

        result shouldBeEqualTo aRoomReplyMessageEvent(
            replyingTo = originalMessage.message,
            message = aMatrixRoomMessageEvent(
                eventId = replyMessage.id,
                utcTimestamp = replyMessage.utcTimestamp,
                content = A_REPLY_EVENT_MESSAGE,
                author = A_SENDER,
            ),
        )
    }

    private fun givenLookup(event: ApiTimelineEvent.TimelineMessage): suspend (EventId) -> LookupResult {
        return {
            if (it == event.id) LookupResult(event, roomEvent = null) else throw IllegalArgumentException("unexpected id: $it")
        }
    }

    private fun givenLookup(event: RoomEvent): suspend (EventId) -> LookupResult {
        return {
            if (it == event.eventId) LookupResult(apiTimelineEvent = null, roomEvent = event) else throw IllegalArgumentException("unexpected id: $it")
        }
    }
}

private fun ApiTimelineEvent.TimelineMessage.toEditEvent(newTimestamp: Long, messageContent: String) = this.copy(
    id = anEventId("a-new-event-id"),
    utcTimestamp = newTimestamp,
    content = aTimelineTextEventContent(
        body = " * $messageContent",
        relation = anEditRelation(this.id),
    )
)

private fun RoomEvent.Message.toEditEvent(newTimestamp: Long, messageContent: String) = anApiTimelineTextEvent(
    id = anEventId("a-new-event-id"),
    utcTimestamp = newTimestamp,
    content = aTimelineTextEventContent(
        body = " * $messageContent",
        relation = anEditRelation(this.eventId),
    )
)

private fun ApiTimelineEvent.TimelineMessage.toReplyEvent(messageContent: String) = anApiTimelineTextEvent(
    id = anEventId("a-new-event-id"),
    content = aTimelineTextEventContent(
        body = "${this.content} $messageContent",
        formattedBody = "<mx-reply>${this.content}</mx-reply>$messageContent",
        relation = aReplyRelation(this.id),
    )
)

private fun RoomEvent.Message.toReplyEvent(messageContent: String) = anApiTimelineTextEvent(
    id = anEventId("a-new-event-id"),
    content = aTimelineTextEventContent(
        body = "${this.content} $messageContent",
        formattedBody = "<mx-reply>${this.content}</mx-reply>$messageContent",
        relation = aReplyRelation(this.eventId),
    )
)

private fun ApiEncryptedContent.toMegolm(): RoomEvent.Encrypted.MegOlmV1 {
    require(this is ApiEncryptedContent.MegOlmV1)
    return aMegolmV1(this.cipherText, this.deviceId, this.senderKey, this.sessionId)
}

private class FakeLookup(private val result: LookupResult) : suspend (EventId) -> LookupResult {
    override suspend fun invoke(p1: EventId) = result
}

private fun ApiTimelineEvent.TimelineMessage.asTextContent() = this.content as ApiTimelineEvent.TimelineMessage.Content.Text
