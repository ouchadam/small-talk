package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.*
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

    suspend fun processRoom(roomToProcess: RoomToProcess, isInitialSync: Boolean): RoomState? {
        val members = roomToProcess.apiSyncRoom.collectMembers(roomToProcess.userCredentials)
        roomMembersService.insert(roomToProcess.roomId, members)

        roomToProcess.apiSyncRoom.timeline.apiTimelineEvents.filterIsInstance<ApiTimelineEvent.RoomRedcation>().forEach {
            roomDataSource.redact(roomToProcess.roomId, it.redactedId)
        }

        val previousState = roomDataSource.read(roomToProcess.roomId)

        val (newEvents, distinctEvents) = timelineEventsProcessor.process(
            roomToProcess,
            previousState?.events ?: emptyList(),
        )

        return createRoomOverview(distinctEvents, roomToProcess, previousState)?.let {
            unreadEventsProcessor.processUnreadState(it, previousState?.roomOverview, newEvents, roomToProcess.userCredentials.userId, isInitialSync)

            RoomState(it, distinctEvents).also {
                roomDataSource.persist(roomToProcess.roomId, previousState, it)
                ephemeralEventsUseCase.processEvents(roomToProcess)
            }
        }
    }

    private suspend fun createRoomOverview(distinctEvents: List<RoomEvent>, roomToProcess: RoomToProcess, previousState: RoomState?): RoomOverview? {
        val lastMessage = distinctEvents.sortedByDescending { it.utcTimestamp }.findLastMessage()
        return roomOverviewProcessor.process(roomToProcess, previousState?.roomOverview, lastMessage)
    }

}

private fun ApiSyncRoom.collectMembers(userCredentials: UserCredentials): List<RoomMember> {
    return (this.state?.stateEvents.orEmpty() + this.timeline.apiTimelineEvents)
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
    return this.firstOrNull()?.let {
        LastMessage(
            content = it.toTextContent(),
            utcTimestamp = it.utcTimestamp,
            author = it.author,
        )
    }
}

private fun RoomEvent.toTextContent(): String = when (this) {
    is RoomEvent.Image -> "\uD83D\uDCF7"
    is RoomEvent.Message -> this.content.asString()
    is RoomEvent.Reply -> this.message.toTextContent()
    is RoomEvent.Encrypted -> "Encrypted message"
    is RoomEvent.Redacted -> "Message deleted"
}