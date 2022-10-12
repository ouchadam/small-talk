package app.dapk.st.engine

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.clearAndPutAll
import app.dapk.st.core.extensions.containsKey
import app.dapk.st.core.log
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomStore
import kotlinx.coroutines.flow.*
import app.dapk.st.matrix.sync.RoomEvent as MatrixRoomEvent
import app.dapk.st.matrix.sync.RoomOverview as MatrixRoomOverview

internal typealias ObserveUnreadNotificationsUseCase = () -> Flow<UnreadNotifications>

class ObserveUnreadNotificationsUseCaseImpl(private val roomStore: RoomStore) : ObserveUnreadNotificationsUseCase {

    override fun invoke(): Flow<UnreadNotifications> {
        return roomStore.observeUnread()
            .mapWithDiff()
            .avoidShowingPreviousNotificationsOnLaunch()
            .onlyRenderableChanges()
    }

}

private fun Flow<Map<MatrixRoomOverview, List<MatrixRoomEvent>>>.mapWithDiff(): Flow<Pair<Map<MatrixRoomOverview, List<MatrixRoomEvent>>, NotificationDiff>> {
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

private fun Map<MatrixRoomOverview, List<MatrixRoomEvent>>.toTimestampedIds() = this
    .mapValues { it.value.toEventIds() }
    .mapKeys { it.key.roomId }

private fun List<MatrixRoomEvent>.toEventIds() = this.map { it.eventId to it.utcTimestamp }

private fun <T> Flow<T>.avoidShowingPreviousNotificationsOnLaunch() = drop(1)

private fun Flow<Pair<Map<MatrixRoomOverview, List<MatrixRoomEvent>>, NotificationDiff>>.onlyRenderableChanges(): Flow<UnreadNotifications> {
    val inferredCurrentNotifications = mutableMapOf<RoomId, List<MatrixRoomEvent>>()
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
        .map {
            val engineModels = it.first
                .mapKeys { it.key.engine() }
                .mapValues { it.value.map { it.engine() } }
            engineModels to it.second
        }
}

typealias TimestampedEventId = Pair<EventId, Long>