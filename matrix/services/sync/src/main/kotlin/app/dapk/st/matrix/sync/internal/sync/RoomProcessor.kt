package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.convertMxUrToUrl
import app.dapk.st.matrix.sync.*
import app.dapk.st.matrix.sync.internal.request.ApiSyncRoom
import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent

internal class RoomProcessor(
    private val roomMembersService: RoomMembersService,
    private val roomDataSource: RoomDataSource,
    private val timelineEventsProcessor: TimelineEventsProcessor,
    private val roomOverviewProcessor: RoomOverviewProcessor,
    private val unreadEventsProcessor: UnreadEventsProcessor,
    private val ephemeralEventsUseCase: EphemeralEventsUseCase,
) {

    suspend fun processRoom(roomToProcess: RoomToProcess, isInitialSync: Boolean): RoomState {
        val members = roomToProcess.apiSyncRoom.collectMembers(roomToProcess.userCredentials)
        roomMembersService.insert(roomToProcess.roomId, members)

        val previousState = roomDataSource.read(roomToProcess.roomId)

        val (newEvents, distinctEvents) = timelineEventsProcessor.process(
            roomToProcess,
            previousState?.events ?: emptyList(),
        )

        val overview = createRoomOverview(distinctEvents, roomToProcess, previousState)
        unreadEventsProcessor.processUnreadState(overview, previousState?.roomOverview, newEvents, roomToProcess.userCredentials.userId, isInitialSync)

        return RoomState(overview, distinctEvents).also {
            roomDataSource.persist(roomToProcess.roomId, previousState, it)
            ephemeralEventsUseCase.processEvents(roomToProcess)
        }
    }

    private suspend fun createRoomOverview(distinctEvents: List<RoomEvent>, roomToProcess: RoomToProcess, previousState: RoomState?): RoomOverview {
        val lastMessage = distinctEvents.sortedByDescending { it.utcTimestamp }.findLastMessage()
        return roomOverviewProcessor.process(roomToProcess, previousState?.roomOverview, lastMessage)
    }

}

private fun ApiSyncRoom.collectMembers(userCredentials: UserCredentials): List<RoomMember> {
    return (this.state.stateEvents + this.timeline.apiTimelineEvents)
        .filterIsInstance<ApiTimelineEvent.RoomMember>()
        .mapNotNull {
            when {
                it.content.membership.isJoin() -> {
                    RoomMember(
                        displayName = it.content.displayName,
                        id = it.senderId,
                        avatarUrl = it.content.avatarUrl?.convertMxUrToUrl(userCredentials.homeServer)?.let { AvatarUrl(it) },
                    )
                }
                else -> null
            }
        }
}


internal fun List<RoomEvent>.findLastMessage(): LastMessage? {
    return this.filterIsInstance<RoomEvent.Message>().firstOrNull()?.let {
        LastMessage(
            content = it.content,
            utcTimestamp = it.utcTimestamp,
            author = it.author,
        )
    }
}
