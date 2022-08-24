package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent

internal class EventLookupUseCase(
    private val roomStore: RoomStore,
) {

    suspend fun lookup(eventId: EventId, decryptedTimeline: DecryptedTimeline, decryptedPreviousEvents: DecryptedRoomEvents): LookupResult {
        return decryptedTimeline.lookup(eventId)
            ?: decryptedPreviousEvents.lookup(eventId)
            ?: lookupFromPersistence(eventId)
            ?: LookupResult(apiTimelineEvent = null, roomEvent = null)
    }

    private fun DecryptedTimeline.lookup(id: EventId) = this.value
        .filterIsInstance<ApiTimelineEvent.TimelineMessage>()
        .firstOrNull { it.id == id }
        ?.let { LookupResult(apiTimelineEvent = it, roomEvent = null) }

    private fun DecryptedRoomEvents.lookup(id: EventId) = this.value
        .firstOrNull { it.eventId == id }
        ?.let { LookupResult(apiTimelineEvent = null, roomEvent = it) }

    private suspend fun lookupFromPersistence(eventId: EventId) = roomStore.findEvent(eventId)?.let {
        LookupResult(apiTimelineEvent = null, roomEvent = it)
    }
}