package app.dapk.st.notifications

import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RenderNotificationsUseCase(
    private val notificationRenderer: NotificationMessageRenderer,
    private val inviteRenderer: NotificationInviteRenderer,
    private val observeRenderableUnreadEventsUseCase: ObserveUnreadNotificationsUseCase,
    private val observeInviteNotificationsUseCase: ObserveInviteNotificationsUseCase,
    private val notificationChannels: NotificationChannels,
) {

    suspend fun listenForNotificationChanges(scope: CoroutineScope) {
        notificationChannels.initChannels()
        observeRenderableUnreadEventsUseCase()
            .onEach { (each, diff) -> renderUnreadChange(each, diff) }
            .launchIn(scope)

        observeInviteNotificationsUseCase()
            .onEach { inviteRenderer.render(it) }
            .launchIn(scope)
    }

    private suspend fun renderUnreadChange(allUnread: Map<RoomOverview, List<RoomEvent>>, diff: NotificationDiff) {
        notificationRenderer.render(
            NotificationState(
                allUnread = allUnread,
                removedRooms = diff.removed.keys,
                roomsWithNewEvents = diff.changedOrNew.keys,
                newRooms = diff.newRooms,
            )
        )
    }
}
