package app.dapk.st.notifications

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.clearAndPutAll
import app.dapk.st.core.extensions.containsKey
import app.dapk.st.core.log
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.RoomStore
import kotlinx.coroutines.flow.*

typealias UnreadNotifications = Pair<Map<RoomOverview, List<RoomEvent>>, NotificationDiff>
internal typealias ObserveUnreadNotificationsUseCase = suspend () -> Flow<UnreadNotifications>

class ObserveUnreadNotificationsUseCaseImpl(private val roomStore: RoomStore) : ObserveUnreadNotificationsUseCase {

    override suspend fun invoke(): Flow<UnreadNotifications> {
        return roomStore.observeUnread()
            .mapWithDiff()
            .avoidShowingPreviousNotificationsOnLaunch()
            .onlyRenderableChanges()
    }

}

private fun Flow<UnreadNotifications>.onlyRenderableChanges(): Flow<UnreadNotifications> {
    val inferredCurrentNotifications = mutableMapOf<RoomId, List<RoomEvent>>()
    return this
        .filter { (_, diff) ->
            when {
                diff.changedOrNew.isEmpty() && diff.removed.isEmpty() -> {
                    log(AppLogTag.NOTIFICATION, "Ignoring unread change due to no renderable changes")
                    false
                }

                inferredCurrentNotifications.isEmpty() && diff.removed.isNotEmpty() -> {
                    log(AppLogTag.NOTIFICATION, "Ignoring unread change due to no currently showing messages and changes are all messages marked as read")
                    false
                }

                else -> true
            }
        }
        .onEach { (allUnread, _) -> inferredCurrentNotifications.clearAndPutAll(allUnread.mapKeys { it.key.roomId }) }
}

private fun Flow<Map<RoomOverview, List<RoomEvent>>>.mapWithDiff(): Flow<Pair<Map<RoomOverview, List<RoomEvent>>, NotificationDiff>> {
    val previousUnreadEvents = mutableMapOf<RoomId, List<TimestampedEventId>>()
    return this.map { each ->
        val allUnreadIds = each.toTimestampedIds()
        val notificationDiff = calculateDiff(allUnreadIds, previousUnreadEvents)
        previousUnreadEvents.clearAndPutAll(allUnreadIds)
        each to notificationDiff
    }
}

private fun calculateDiff(allUnread: Map<RoomId, List<TimestampedEventId>>, previousUnread: Map<RoomId, List<TimestampedEventId>>?): NotificationDiff {
    val previousLatestEventTimestamps = previousUnread.toLatestTimestamps()
    val newRooms = allUnread.filter { !previousUnread.containsKey(it.key) }.keys

    val unchanged = previousUnread?.filter {
        allUnread.containsKey(it.key) && (it.value == allUnread[it.key])
    } ?: emptyMap()
    val changedOrNew = allUnread.filterNot { unchanged.containsKey(it.key) }.mapValues { (key, value) ->
        val isChangedRoom = !newRooms.contains(key)
        if (isChangedRoom) {
            val latest = previousLatestEventTimestamps[key] ?: 0L
            value.filter {
                val isExistingEvent = (previousUnread?.get(key)?.contains(it) ?: false)
                !isExistingEvent && it.second > latest
            }
        } else {
            value
        }
    }.filter { it.value.isNotEmpty() }
    val removed = previousUnread?.filter { !allUnread.containsKey(it.key) } ?: emptyMap()
    return NotificationDiff(unchanged.toEventIds(), changedOrNew.toEventIds(), removed.toEventIds(), newRooms)
}

private fun Map<RoomId, List<TimestampedEventId>>?.toLatestTimestamps() = this?.mapValues { it.value.maxOf { it.second } } ?: emptyMap()

private fun Map<RoomId, List<TimestampedEventId>>.toEventIds() = this.mapValues { it.value.map { it.first } }

private fun Map<RoomOverview, List<RoomEvent>>.toTimestampedIds() = this
    .mapValues { it.value.toEventIds() }
    .mapKeys { it.key.roomId }

private fun List<RoomEvent>.toEventIds() = this.map { it.eventId to it.utcTimestamp }

private fun <T> Flow<T>.avoidShowingPreviousNotificationsOnLaunch() = drop(1)

data class NotificationDiff(
    val unchanged: Map<RoomId, List<EventId>>,
    val changedOrNew: Map<RoomId, List<EventId>>,
    val removed: Map<RoomId, List<EventId>>,
    val newRooms: Set<RoomId>
)

typealias TimestampedEventId = Pair<EventId, Long>