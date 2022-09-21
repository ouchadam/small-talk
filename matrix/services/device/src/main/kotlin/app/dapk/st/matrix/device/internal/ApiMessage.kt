package app.dapk.st.matrix.device.internal

import app.dapk.st.matrix.common.MessageType
import app.dapk.st.matrix.common.RoomId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ApiMessage {

    @Serializable
    @SerialName("text_message")
    data class TextMessage(
        @SerialName("content") val content: TextContent,
        @SerialName("room_id") val roomId: RoomId,
        @SerialName("type") val type: String,
    ) : ApiMessage() {

        @Serializable
        data class TextContent(
            @SerialName("body") val body: String,
            @SerialName("msgtype") val type: String = MessageType.TEXT.value,
        )
    }

}