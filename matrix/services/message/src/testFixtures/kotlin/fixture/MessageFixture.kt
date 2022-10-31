package fixture

import app.dapk.st.matrix.common.MessageType
import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.message.MessageService

fun aTextMessage(
    content: MessageService.Message.Content.TextContent = aTextContent(),
    sendEncrypted: Boolean = false,
    roomId: RoomId = aRoomId(),
    localId: String = "a-local-id",
    timestampUtc: Long = 0,
) = MessageService.Message.TextMessage(content, sendEncrypted, roomId, localId, timestampUtc)

fun aTextContent(
    body: RichText = RichText.of("text content body"),
    type: String = MessageType.TEXT.value,
) = MessageService.Message.Content.TextContent(body, type)
