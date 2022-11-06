package app.dapk.st.engine

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.message.internal.ImageContentReader
import fake.FakeLocalIdFactory
import fixture.aRoomMember
import fixture.aRoomOverview
import fixture.anEventId
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import test.delegateReturn
import test.runExpectTest
import java.time.Clock

private const val AN_IMAGE_URI = ""
private val AN_IMAGE_META = ImageContentReader.ImageContent(
    height = 50,
    width = 100,
    size = 1000L,
    fileName = "a file name",
    mimeType = "image/png"
)
private const val A_CURRENT_TIME = 2000L
private const val A_LOCAL_ID = "a local id"
private val A_ROOM_OVERVIEW = aRoomOverview(
    isEncrypted = true
)
private val A_REPLY = SendMessage.TextMessage.Reply(
    aRoomMember(),
    originalMessage = "",
    anEventId(),
    timestampUtc = 7000
)
private const val A_TEXT_MESSAGE_CONTENT = "message content"

class SendMessageUseCaseTest {

    private val fakeMessageService = FakeMessageService()
    private val fakeLocalIdFactory = FakeLocalIdFactory().apply { givenCreate().returns(A_LOCAL_ID) }
    private val fakeImageContentReader = FakeImageContentReader()
    private val fakeClock = FakeClock().apply { givenMillis().returns(A_CURRENT_TIME) }

    private val useCase = SendMessageUseCase(
        fakeMessageService,
        fakeLocalIdFactory.instance,
        fakeImageContentReader,
        fakeClock.instance
    )

    @Test
    fun `when sending image message, then schedules message`() = runExpectTest {
        fakeImageContentReader.givenMeta(AN_IMAGE_URI).returns(AN_IMAGE_META)
        val expectedImageMessage = createExpectedImageMessage(A_ROOM_OVERVIEW)
        fakeMessageService.expect { it.scheduleMessage(expectedImageMessage) }

        useCase.send(SendMessage.ImageMessage(uri = AN_IMAGE_URI), A_ROOM_OVERVIEW)

        verifyExpects()
    }

    @Test
    fun `when sending text message, then schedules message`() = runExpectTest {
        val expectedTextMessage = createExpectedTextMessage(A_ROOM_OVERVIEW, A_TEXT_MESSAGE_CONTENT, reply = null)
        fakeMessageService.expect { it.scheduleMessage(expectedTextMessage) }

        useCase.send(
            SendMessage.TextMessage(
                content = A_TEXT_MESSAGE_CONTENT,
                reply = null,
            ),
            A_ROOM_OVERVIEW
        )

        verifyExpects()
    }

    @Test
    fun `given a reply, when sending text message, then schedules message with reply`() = runExpectTest {
        val expectedTextMessage = createExpectedTextMessage(A_ROOM_OVERVIEW, A_TEXT_MESSAGE_CONTENT, reply = A_REPLY)
        fakeMessageService.expect { it.scheduleMessage(expectedTextMessage) }

        useCase.send(
            SendMessage.TextMessage(
                content = A_TEXT_MESSAGE_CONTENT,
                reply = A_REPLY,
            ),
            A_ROOM_OVERVIEW
        )

        verifyExpects()
    }


    private fun createExpectedImageMessage(roomOverview: RoomOverview) = MessageService.Message.ImageMessage(
        MessageService.Message.Content.ImageContent(
            uri = AN_IMAGE_URI,
            MessageService.Message.Content.ImageContent.Meta(
                height = AN_IMAGE_META.height,
                width = AN_IMAGE_META.width,
                size = AN_IMAGE_META.size,
                fileName = AN_IMAGE_META.fileName,
                mimeType = AN_IMAGE_META.mimeType,
            )
        ),
        roomId = roomOverview.roomId,
        sendEncrypted = roomOverview.isEncrypted,
        localId = A_LOCAL_ID,
        timestampUtc = A_CURRENT_TIME,
    )

    private fun createExpectedTextMessage(roomOverview: RoomOverview, messageContent: String, reply: SendMessage.TextMessage.Reply?) =
        MessageService.Message.TextMessage(
            content = MessageService.Message.Content.TextContent(RichText.of(messageContent)),
            roomId = roomOverview.roomId,
            sendEncrypted = roomOverview.isEncrypted,
            localId = A_LOCAL_ID,
            timestampUtc = A_CURRENT_TIME,
            reply = reply?.let {
                MessageService.Message.TextMessage.Reply(
                    author = it.author,
                    originalMessage = RichText.of(it.originalMessage),
                    replyContent = messageContent,
                    eventId = it.eventId,
                    timestampUtc = it.timestampUtc,
                )
            }
        )
}

class FakeImageContentReader : ImageContentReader by mockk() {
    fun givenMeta(uri: String) = every { meta(uri) }.delegateReturn()
}

class FakeClock {
    val instance = mockk<Clock>()
    fun givenMillis() = every { instance.millis() }.delegateReturn()
}
