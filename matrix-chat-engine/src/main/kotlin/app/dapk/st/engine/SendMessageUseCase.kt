package app.dapk.st.engine

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.message.internal.ImageContentReader
import java.time.Clock

internal class SendMessageUseCase(
    private val messageService: MessageService,
    private val localIdFactory: LocalIdFactory,
    private val imageContentReader: ImageContentReader,
    private val clock: Clock,
) {

    suspend fun send(message: SendMessage, room: RoomOverview) {
        when (message) {
            is SendMessage.ImageMessage -> createImageMessage(message, room)
            is SendMessage.TextMessage -> messageService.scheduleMessage(createTextMessage(message, room))
        }
    }

    private suspend fun createImageMessage(message: SendMessage.ImageMessage, room: RoomOverview) {
        val meta = imageContentReader.meta(message.uri)
        messageService.scheduleMessage(
            MessageService.Message.ImageMessage(
                MessageService.Message.Content.ImageContent(
                    uri = message.uri,
                    MessageService.Message.Content.ImageContent.Meta(
                        height = meta.height,
                        width = meta.width,
                        size = meta.size,
                        fileName = meta.fileName,
                        mimeType = meta.mimeType,
                    )
                ),
                roomId = room.roomId,
                sendEncrypted = room.isEncrypted,
                localId = localIdFactory.create(),
                timestampUtc = clock.millis(),
            )
        )
    }

    private fun createTextMessage(message: SendMessage.TextMessage, room: RoomOverview) = MessageService.Message.TextMessage(
        content = MessageService.Message.Content.TextContent(RichText.of(message.content)),
        roomId = room.roomId,
        sendEncrypted = room.isEncrypted,
        localId = localIdFactory.create(),
        timestampUtc = clock.millis(),
        reply = message.reply?.let {
            MessageService.Message.TextMessage.Reply(
                author = it.author,
                originalMessage = RichText.of(it.originalMessage),
                replyContent = message.content,
                eventId = it.eventId,
                timestampUtc = it.timestampUtc,
            )
        }
    )

}