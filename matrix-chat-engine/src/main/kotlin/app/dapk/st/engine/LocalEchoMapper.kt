package app.dapk.st.engine

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.message.MessageService

internal class LocalEchoMapper(private val metaMapper: MetaMapper) {

    fun MessageService.LocalEcho.toMessage(member: RoomMember): RoomEvent {
        return when (val message = this.message) {
            is MessageService.Message.TextMessage -> {
                val mappedMessage = RoomEvent.Message(
                    eventId = this.eventId ?: EventId(this.localId),
                    content = message.content.body,
                    author = member,
                    utcTimestamp = message.timestampUtc,
                    meta = metaMapper.toMeta(this)
                )

                when (val reply = message.reply) {
                    null -> mappedMessage
                    else -> RoomEvent.Reply(
                        mappedMessage, RoomEvent.Message(
                            eventId = reply.eventId,
                            content = reply.originalMessage,
                            author = reply.author,
                            utcTimestamp = reply.timestampUtc,
                            meta = MessageMeta.FromServer
                        )
                    )
                }
            }

            is MessageService.Message.ImageMessage -> {
                RoomEvent.Image(
                    eventId = this.eventId ?: EventId(this.localId),
                    author = member,
                    utcTimestamp = message.timestampUtc,
                    meta = metaMapper.toMeta(this),
                    imageMeta = RoomEvent.Image.ImageMeta(message.content.meta.width, message.content.meta.height, message.content.uri, null),
                )
            }
        }
    }

    fun RoomEvent.mergeWith(echo: MessageService.LocalEcho): RoomEvent = when (this) {
        is RoomEvent.Message -> this.copy(meta = metaMapper.toMeta(echo))
        is RoomEvent.Reply -> this.copy(message = this.message.mergeWith(echo))
        is RoomEvent.Image -> this.copy(meta = metaMapper.toMeta(echo))
        is RoomEvent.Encrypted -> this.copy(meta = metaMapper.toMeta(echo))
        is RoomEvent.Redacted -> this
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