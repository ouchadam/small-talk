package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.RoomId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ApiFilterResponse(
    @SerialName("filter_id") val id: String
)

@Serializable
internal data class FilterRequest(
    @SerialName("event_fields") val eventFields: List<String>? = null,
    @SerialName("room") val roomFilter: RoomFilter? = null,
    @SerialName("account_data") val account: EventFilter? = null,
)

@Serializable
internal data class RoomFilter(
    @SerialName("rooms") val rooms: List<RoomId>? = null,
    @SerialName("timeline") val timelineFilter: RoomEventFilter? = null,
    @SerialName("state") val stateFilter: RoomEventFilter? = null,
    @SerialName("ephemeral") val ephemeralFilter: RoomEventFilter? = null,
    @SerialName("account_data") val accountFilter: RoomEventFilter? = null,
)

@Serializable
internal data class RoomEventFilter(
    @SerialName("limit") val limit: Int? = null,
    @SerialName("types") val types: List<String>? = null,
    @SerialName("rooms") val rooms: List<RoomId>? = null,
    @SerialName("lazy_load_members") val lazyLoadMembers: Boolean = false,
)

@Serializable
internal data class EventFilter(
    @SerialName("limit") val limit: Int? = null,
    @SerialName("not_types") val notTypes: List<String>? = null,
    @SerialName("types") val types: List<String>? = null,
)