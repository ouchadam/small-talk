package app.dapk.st.matrix.sync.internal.room

import app.dapk.st.matrix.common.EncryptedMessageContent
import app.dapk.st.matrix.common.JsonString
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.request.DecryptedContent
import fake.FakeMatrixLogger
import fake.FakeMessageDecrypter
import fixture.*
import internalfixture.aTimelineTextEventContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_DECRYPTED_MESSAGE_CONTENT = "decrypted - content"
private val AN_ENCRYPTED_ROOM_CONTENT = aMegolmV1()
private val AN_ENCRYPTED_ROOM_MESSAGE = aMatrixRoomMessageEvent(encryptedContent = AN_ENCRYPTED_ROOM_CONTENT)
private val AN_ENCRYPTED_ROOM_REPLY = aRoomReplyMessageEvent(
    message = AN_ENCRYPTED_ROOM_MESSAGE,
    replyingTo = AN_ENCRYPTED_ROOM_MESSAGE.copy(eventId = anEventId("other-event"))
)
private val A_DECRYPTED_CONTENT = DecryptedContent.TimelineText(aTimelineTextEventContent(body = A_DECRYPTED_MESSAGE_CONTENT))
private val A_USER_CREDENTIALS = aUserCredentials()

private val json = Json { encodeDefaults = true }

class RoomEventsDecrypterTest {

    private val fakeMessageDecrypter = FakeMessageDecrypter()

    private val roomEventsDecrypter = RoomEventsDecrypter(
        fakeMessageDecrypter,
        Json,
        FakeMatrixLogger(),
    )

    @Test
    fun `given clear message event, when decrypting, then does nothing`() = runTest {
        val aClearMessageEvent = aMatrixRoomMessageEvent(encryptedContent = null)
        val result = roomEventsDecrypter.decryptRoomEvents(A_USER_CREDENTIALS, listOf(aClearMessageEvent))

        result shouldBeEqualTo listOf(aClearMessageEvent)
    }

    @Test
    fun `given encrypted message event, when decrypting, then applies decrypted body and removes encrypted content`() = runTest {
        givenEncryptedMessage(AN_ENCRYPTED_ROOM_MESSAGE, decryptsTo = A_DECRYPTED_CONTENT)

        val result = roomEventsDecrypter.decryptRoomEvents(A_USER_CREDENTIALS, listOf(AN_ENCRYPTED_ROOM_MESSAGE))

        result shouldBeEqualTo listOf(AN_ENCRYPTED_ROOM_MESSAGE.copy(content = A_DECRYPTED_MESSAGE_CONTENT, encryptedContent = null))
    }

    @Test
    fun `given encrypted reply event, when decrypting, then decrypts message and replyTo`() = runTest {
        givenEncryptedReply(AN_ENCRYPTED_ROOM_REPLY, decryptsTo = A_DECRYPTED_CONTENT)

        val result = roomEventsDecrypter.decryptRoomEvents(A_USER_CREDENTIALS, listOf(AN_ENCRYPTED_ROOM_REPLY))

        result shouldBeEqualTo listOf(
            AN_ENCRYPTED_ROOM_REPLY.copy(
                message = (AN_ENCRYPTED_ROOM_REPLY.message as RoomEvent.Message).copy(content = A_DECRYPTED_MESSAGE_CONTENT, encryptedContent = null),
                replyingTo = (AN_ENCRYPTED_ROOM_REPLY.replyingTo as RoomEvent.Message).copy(content = A_DECRYPTED_MESSAGE_CONTENT, encryptedContent = null),
            )
        )
    }

    private fun givenEncryptedMessage(roomMessage: RoomEvent.Message, decryptsTo: DecryptedContent) {
        val model = roomMessage.encryptedContent!!.toModel()
        fakeMessageDecrypter.givenDecrypt(model)
            .returns(aDecryptionSuccessResult(payload = JsonString(json.encodeToString(DecryptedContent.serializer(), decryptsTo))))
    }

    private fun givenEncryptedReply(roomReply: RoomEvent.Reply, decryptsTo: DecryptedContent) {
        givenEncryptedMessage(roomReply.message as RoomEvent.Message, decryptsTo)
        givenEncryptedMessage(roomReply.replyingTo as RoomEvent.Message, decryptsTo)
    }
}

private fun RoomEvent.Message.MegOlmV1.toModel() = EncryptedMessageContent.MegOlmV1(
    this.cipherText,
    this.deviceId,
    this.senderKey,
    this.sessionId,
)
