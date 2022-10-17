package fake

import app.dapk.st.engine.LocalEchoMapper
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.message.MessageService
import io.mockk.every
import io.mockk.mockk

internal class FakeLocalEventMapper {
    val instance = mockk<LocalEchoMapper>()
    fun givenMapping(echo: MessageService.LocalEcho, roomMember: RoomMember) = every {
        with(instance) { echo.toMessage(roomMember) }
    }
}