package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.ServerKeyCount
import app.dapk.st.matrix.common.SyncToken
import app.dapk.st.matrix.common.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ApiSyncResponse(
    @SerialName("device_lists") val deviceLists: DeviceLists? = null,
    @SerialName("account_data") val accountData: ApiAccountData? = null,
    @SerialName("rooms") val rooms: ApiSyncRooms? = null,
    @SerialName("to_device") val toDevice: ToDevice? = null,
    @SerialName("device_one_time_keys_count") val oneTimeKeysCount: Map<String, ServerKeyCount>? = null,
    @SerialName("next_batch") val nextBatch: SyncToken,
    @SerialName("prev_batch") val prevBatch: SyncToken? = null,
)

@Serializable
data class ApiAccountData(
    @SerialName("events") val events: List<ApiAccountEvent>
)

@Serializable
internal data class DeviceLists(
    @SerialName("changed") val changed: List<UserId>? = null
)

@Serializable
internal data class ToDevice(
    @SerialName("events") val events: List<ApiToDeviceEvent>
)

@Serializable
internal data class ApiSyncRooms(
    @SerialName("join") val join: Map<RoomId, ApiSyncRoom>? = null,
    @SerialName("invite") val invite: Map<RoomId, ApiSyncRoomInvite>? = null,
    @SerialName("leave") val leave: Map<RoomId, ApiSyncRoom>? = null,
)

@Serializable
internal data class ApiSyncRoomInvite(
    @SerialName("invite_state") val state: ApiInviteEvents,
)

@Serializable
internal data class ApiInviteEvents(
    @SerialName("events") val events: List<ApiStrippedEvent>
)

@Serializable
internal data class ApiSyncRoom(
    @SerialName("timeline") val timeline: ApiSyncRoomTimeline,
    @SerialName("state") val state: ApiSyncRoomState? = null,
    @SerialName("account_data") val accountData: ApiAccountData? = null,
    @SerialName("ephemeral") val ephemeral: ApiEphemeral? = null,
    @SerialName("summary") val summary: ApiRoomSummary? = null,
)

@Serializable
internal data class ApiRoomSummary(
    @SerialName("m.heroes") val heroes: List<UserId>? = null
)

@Serializable
internal data class ApiEphemeral(
    @SerialName("events") val events: List<ApiEphemeralEvent>
)

@Serializable
internal sealed class ApiEphemeralEvent {

    @Serializable
    @SerialName("m.typing")
    internal data class Typing(
        @SerialName("content") val content: Content,
    ) : ApiEphemeralEvent() {
        @Serializable
        internal data class Content(
            @SerialName("user_ids") val userIds: List<UserId>
        )
    }

    @Serializable
    object Ignored : ApiEphemeralEvent()
}


@Serializable
internal data class ApiSyncRoomState(
    @SerialName("events") val stateEvents: List<ApiTimelineEvent>,
)

@Serializable
internal data class ApiSyncRoomTimeline(
    @SerialName("events") val apiTimelineEvents: List<ApiTimelineEvent>,
)


@Serializable
internal sealed class DecryptedContent {

    @Serializable
    @SerialName("m.room.message")
    internal data class TimelineText(
        @SerialName("content") val content: ApiTimelineEvent.TimelineMessage.Content,
    ) : DecryptedContent()

    @Serializable
    object Ignored : DecryptedContent()
}
