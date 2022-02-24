package app.dapk.st.matrix.message

import app.dapk.st.matrix.common.EventId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiSendResponse(
    @SerialName("event_id") val eventId: EventId,
)