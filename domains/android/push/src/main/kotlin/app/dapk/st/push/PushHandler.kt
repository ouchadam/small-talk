package app.dapk.st.push

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushTokenPayload(
    @SerialName("token") val token: String,
    @SerialName("gateway_url") val gatewayUrl: String,
)

interface PushHandler {
    fun onNewToken(payload: PushTokenPayload)
    fun onMessageReceived(eventId: EventId?, roomId: RoomId?)
}