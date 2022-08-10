package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ApiAccountEvent {

    @Serializable
    @SerialName("m.direct")
    data class Direct(
        @SerialName("content") val content: Map<UserId, List<RoomId>>
    ) : ApiAccountEvent()

    @Serializable
    @SerialName("m.fully_read")
    data class FullyRead(
        @SerialName("content") val content: Content,
    ) : ApiAccountEvent() {

        @Serializable
        data class Content(
            @SerialName("event_id") val eventId: EventId,
        )

    }

    @Serializable
    object Ignored : ApiAccountEvent()
}