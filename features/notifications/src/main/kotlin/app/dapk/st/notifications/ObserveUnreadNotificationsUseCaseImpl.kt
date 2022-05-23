package app.dapk.st.notifications

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.clearAndPutAll
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
    val previousUnreadEvents = mutableMapOf<RoomId, List<EventId>>()
    return this.map { each ->
        val allUnreadIds = each.toIds()
        val notificationDiff = calculateDiff(allUnreadIds, previousUnreadEvents)
        previousUnreadEvents.clearAndPutAll(allUnreadIds)
        each to notificationDiff
    }
}

private fun calculateDiff(allUnread: Map<RoomId, List<EventId>>, previousUnread: Map<RoomId, List<EventId>>?): NotificationDiff {
    val unchanged = previousUnread?.filter { allUnread.containsKey(it.key) && it.value == allUnread[it.key] } ?: emptyMap()
    val changedOrNew = allUnread.filterNot { unchanged.containsKey(it.key) }
    val removed = previousUnread?.filter { !allUnread.containsKey(it.key) } ?: emptyMap()
    return NotificationDiff(unchanged, changedOrNew, removed)
}

private fun List<RoomEvent>.toEventIds() = this.map { it.eventId }

private fun Map<RoomOverview, List<RoomEvent>>.toIds() = this
    .mapValues { it.value.toEventIds() }
    .mapKeys { it.key.roomId }

private fun <T> Flow<T>.avoidShowingPreviousNotificationsOnLaunch() = drop(1)

data class NotificationDiff(
    val unchanged: Map<RoomId, List<EventId>>,
    val changedOrNew: Map<RoomId, List<EventId>>,
    val removed: Map<RoomId, List<EventId>>
)
