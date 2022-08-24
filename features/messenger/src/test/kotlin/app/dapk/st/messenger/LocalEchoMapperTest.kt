package app.dapk.st.messenger

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.sync.MessageMeta
import fixture.*
import internalfake.FakeMetaMapper
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_META = MessageMeta.LocalEcho("echo-id", MessageMeta.LocalEcho.State.Sent)
private val AN_ECHO_CONTENT = aTextMessage(localId = "a-local-id")
private val A_ROOM_MEMBER = aRoomMember()

class LocalEchoMapperTest {

    private val fakeMetaMapper = FakeMetaMapper()

    private val localEchoMapper = LocalEchoMapper(fakeMetaMapper.instance)

    @Test
    fun `given echo with event id when mapping to message then uses event id`() = runWith(localEchoMapper) {
        val echo = givenEcho(eventId = anEventId("a-known-id"))

        val result = echo.toMessage(A_ROOM_MEMBER)

        result shouldBeEqualTo aRoomMessageEvent(
            eventId = echo.eventId!!,
            content = AN_ECHO_CONTENT.content.body,
            meta = A_META
        )
    }

    @Test
    fun `given echo without event id when mapping to message then uses local id`() = runWith(localEchoMapper) {
        val echo = givenEcho(eventId = null, localId = "a-local-id")

        val result = echo.toMessage(A_ROOM_MEMBER)

        result shouldBeEqualTo aRoomMessageEvent(
            eventId = anEventId(echo.localId),
            content = AN_ECHO_CONTENT.content.body,
            meta = A_META
        )
    }

    @Test
    fun `when merging with echo then updates meta with the echos meta`() = runWith(localEchoMapper) {
        val previousMeta = MessageMeta.LocalEcho("previous", MessageMeta.LocalEcho.State.Sending)
        val event = aRoomMessageEvent(meta = previousMeta)
        val echo = aLocalEcho()
        fakeMetaMapper.given(echo).returns(A_META)

        val result = event.mergeWith(echo)

        result shouldBeEqualTo aRoomMessageEvent(meta = A_META)
    }

    private fun givenEcho(eventId: EventId? = null, localId: String = "", meta: MessageMeta.LocalEcho = A_META): MessageService.LocalEcho {
        return aLocalEcho(eventId = eventId, message = aTextMessage(localId = localId)).also {
            fakeMetaMapper.given(it).returns(meta)
        }
    }
}


fun <T> runWith(context: T, block: T.() -> Unit) {
    block(context)
}
