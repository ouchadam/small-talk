package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent
import app.dapk.st.matrix.sync.internal.room.RoomEventsDecrypter
import app.dapk.st.matrix.sync.internal.room.SyncEventDecrypter

private typealias NewEvents = List<RoomEvent>
private typealias AllDistinctEvents = List<RoomEvent>

internal class TimelineEventsProcessor(
    private val roomEventCreator: RoomEventCreator,
    private val roomEventsDecrypter: RoomEventsDecrypter,
    private val eventDecrypter: SyncEventDecrypter,
    private val eventLookupUseCase: EventLookupUseCase
) {

    suspend fun process(roomToProcess: RoomToProcess, previousEvents: List<RoomEvent>): Pair<NewEvents, AllDistinctEvents> {
        val newEvents = processNewEvents(roomToProcess, previousEvents)
        return newEvents to (newEvents + previousEvents).distinctBy { it.eventId }
    }

    private suspend fun processNewEvents(roomToProcess: RoomToProcess, previousEvents: List<RoomEvent>): List<RoomEvent> {
        val decryptedTimeline = roomToProcess.apiSyncRoom.timeline.apiTimelineEvents.decryptEvents()
        val decryptedPreviousEvents = previousEvents.decryptEvents(roomToProcess.userCredentials)

        val newEvents = with(roomEventCreator) {
            decryptedTimeline.value.mapNotNull { event ->
                val roomEvent = when (event) {
                    is ApiTimelineEvent.Encrypted -> event.toRoomEvent(roomToProcess.roomId)
                    is ApiTimelineEvent.TimelineMessage -> event.toRoomEvent(roomToProcess.userCredentials, roomToProcess.roomId) { eventId ->
                        eventLookupUseCase.lookup(eventId, decryptedTimeline, decryptedPreviousEvents)
                    }
                    is ApiTimelineEvent.RoomRedcation -> null
                    is ApiTimelineEvent.Encryption -> null
                    is ApiTimelineEvent.RoomAvatar -> null
                    is ApiTimelineEvent.RoomCreate -> null
                    is ApiTimelineEvent.RoomMember -> null
                    is ApiTimelineEvent.RoomName -> null
                    is ApiTimelineEvent.RoomTopic -> null
                    is ApiTimelineEvent.CanonicalAlias -> null
                    ApiTimelineEvent.Ignored -> null
                }
                roomEvent
            }
        }
        return newEvents
    }

    private suspend fun List<ApiTimelineEvent>.decryptEvents() = DecryptedTimeline(eventDecrypter.decryptTimelineEvents(this))
    private suspend fun List<RoomEvent>.decryptEvents(userCredentials: UserCredentials) =
        DecryptedRoomEvents(roomEventsDecrypter.decryptRoomEvents(userCredentials, this))

}

@JvmInline
internal value class DecryptedTimeline(val value: List<ApiTimelineEvent>)

@JvmInline
internal value class DecryptedRoomEvents(val value: List<RoomEvent>)
