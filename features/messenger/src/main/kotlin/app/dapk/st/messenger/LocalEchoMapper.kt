package app.dapk.st.messenger

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent

class LocalEchoMapper {

    fun MessageService.LocalEcho.toMessage(message: MessageService.Message.TextMessage, member: RoomMember): RoomEvent.Message {
        return RoomEvent.Message(
            eventId = this.eventId ?: EventId(this.localId),
            content = message.content.body,
            author = member,
            utcTimestamp = message.timestampUtc,
            meta = this.toMeta()
        )
    }

    fun RoomEvent.mergeWith(echo: MessageService.LocalEcho) = when (this) {
        is RoomEvent.Message -> this.copy(meta = echo.toMeta())
        is RoomEvent.Reply -> this.copy(message = this.message.copy(meta = echo.toMeta()))
    }

    private fun MessageService.LocalEcho.toMeta() = MessageMeta.LocalEcho(
        echoId = this.localId,
        state = when (val localEchoState = this.state) {
            MessageService.LocalEcho.State.Sending -> MessageMeta.LocalEcho.State.Sending
            MessageService.LocalEcho.State.Sent -> MessageMeta.LocalEcho.State.Sent
            is MessageService.LocalEcho.State.Error -> MessageMeta.LocalEcho.State.Error(
                localEchoState.message,
                type = MessageMeta.LocalEcho.State.Error.Type.UNKNOWN,
            )
        }
    )
}