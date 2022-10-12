package app.dapk.st.engine

import app.dapk.st.matrix.message.MessageService
import fixture.aLocalEcho
import fixture.aTextMessage
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_LOCAL_ECHO_ID = "a-local-echo-id"

class MetaMapperTest {

    private val metaMapper = MetaMapper()

    @Test
    fun `given echo with sending meta then maps to sending state`() {
        val result = metaMapper.toMeta(
            aLocalEcho(
                state = MessageService.LocalEcho.State.Sending,
                message = aTextMessage(localId = A_LOCAL_ECHO_ID)
            )
        )

        result shouldBeEqualTo MessageMeta.LocalEcho(
            echoId = A_LOCAL_ECHO_ID,
            state = MessageMeta.LocalEcho.State.Sending
        )
    }

    @Test
    fun `given echo with sent meta then maps to sent state`() {
        val result = metaMapper.toMeta(
            aLocalEcho(
                state = MessageService.LocalEcho.State.Sent,
                message = aTextMessage(localId = A_LOCAL_ECHO_ID)
            )
        )

        result shouldBeEqualTo MessageMeta.LocalEcho(
            echoId = A_LOCAL_ECHO_ID,
            state = MessageMeta.LocalEcho.State.Sent
        )
    }

    @Test
    fun `given echo with error meta then maps to error state`() {
        val result = metaMapper.toMeta(
            aLocalEcho(
                state = MessageService.LocalEcho.State.Error("an error", MessageService.LocalEcho.State.Error.Type.UNKNOWN),
                message = aTextMessage(localId = A_LOCAL_ECHO_ID)
            )
        )

        result shouldBeEqualTo MessageMeta.LocalEcho(
            echoId = A_LOCAL_ECHO_ID,
            state = MessageMeta.LocalEcho.State.Error("an error", MessageMeta.LocalEcho.State.Error.Type.UNKNOWN)
        )
    }
}