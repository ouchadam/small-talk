package app.dapk.st.messenger

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent

internal class LocalEchoMapper(private val metaMapper: MetaMapper) {

    fun MessageService.LocalEcho.toMessage(member: RoomMember): RoomEvent.Message {
        return when (val message = this.message) {
            is MessageService.Message.TextMessage -> {
                RoomEvent.Message(
                    eventId = this.eventId ?: EventId(this.localId),
                    content = message.content.body,
                    author = member,
                    utcTimestamp = message.timestampUtc,
                    meta = metaMapper.toMeta(this)
                )
            }
        }
    }

    fun RoomEvent.mergeWith(echo: MessageService.LocalEcho) = when (this) {
        is RoomEvent.Message -> this.copy(meta = metaMapper.toMeta(echo))
        is RoomEvent.Reply -> this.copy(message = this.message.copy(meta = metaMapper.toMeta(echo)))
    }
}

internal class MetaMapper {

    fun toMeta(echo: MessageService.LocalEcho) = MessageMeta.LocalEcho(
        echoId = echo.localId,
        state = when (val localEchoState = echo.state) {
            MessageService.LocalEcho.State.Sending -> MessageMeta.LocalEcho.State.Sending
            MessageService.LocalEcho.State.Sent -> MessageMeta.LocalEcho.State.Sent
            is MessageService.LocalEcho.State.Error -> MessageMeta.LocalEcho.State.Error(
                localEchoState.message,
                type = MessageMeta.LocalEcho.State.Error.Type.UNKNOWN,
            )
        }
    )

}