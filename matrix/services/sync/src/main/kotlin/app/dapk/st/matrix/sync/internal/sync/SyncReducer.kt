package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.withIoContextAsync
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.common.MatrixLogTag.SYNC
import app.dapk.st.matrix.sync.InviteMeta
import app.dapk.st.matrix.sync.RoomInvite
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.matrix.sync.internal.request.*
import app.dapk.st.matrix.sync.internal.room.SideEffectResult
import kotlinx.coroutines.awaitAll

internal class SyncReducer(
    private val roomProcessor: RoomProcessor,
    private val roomRefresher: RoomRefresher,
    private val roomDataSource: RoomDataSource,
    private val logger: MatrixLogger,
    private val errorTracker: ErrorTracker,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    data class ReducerResult(
        val newRoomsJoined: List<RoomId>,
        val roomState: List<RoomState>,
        val invites: List<RoomInvite>,
        val roomsLeft: List<RoomId>
    )

    suspend fun reduce(isInitialSync: Boolean, sideEffects: SideEffectResult, response: ApiSyncResponse, userCredentials: UserCredentials): ReducerResult {
        val directMessages = response.directMessages()
        val invites = response.rooms?.invite?.map { roomInvite(it, userCredentials) } ?: emptyList()
        val roomsLeft = findRoomsLeft(response, userCredentials)
        val newRooms = response.rooms?.join?.keys?.filterNot { roomDataSource.contains(it) } ?: emptyList()

        val apiUpdatedRooms = response.rooms?.join?.keepRoomsWithChanges()
        val apiRoomsToProcess = apiUpdatedRooms?.mapNotNull { (roomId, apiRoom) ->
            logger.matrixLog(SYNC, "reducing: $roomId")
            coroutineDispatchers.withIoContextAsync {
                runCatching {
                    roomProcessor.processRoom(
                        roomToProcess = RoomToProcess(
                            roomId = roomId,
                            apiSyncRoom = apiRoom,
                            directMessage = directMessages[roomId],
                            userCredentials = userCredentials,
                            heroes = apiRoom.summary?.heroes,
                        ),
                        isInitialSync = isInitialSync
                    )
                }
                    .onFailure { errorTracker.track(it, "failed to reduce: $roomId, skipping") }
                    .getOrNull()
            }
        } ?: emptyList()

        val roomsWithSideEffects = sideEffects.roomsToRefresh(alreadyHandledRooms = apiUpdatedRooms?.keys ?: emptySet()).map { roomId ->
            coroutineDispatchers.withIoContextAsync {
                roomRefresher.refreshRoomContent(roomId, userCredentials)
            }
        }

        roomDataSource.remove(roomsLeft)

        return ReducerResult(
            newRooms,
            (apiRoomsToProcess + roomsWithSideEffects).awaitAll().filterNotNull(),
            invites,
            roomsLeft
        )
    }

    private fun findRoomsLeft(response: ApiSyncResponse, userCredentials: UserCredentials) = response.rooms?.leave?.filter {
        it.value.state.stateEvents.filterIsInstance<ApiTimelineEvent.RoomMember>().any {
            it.content.membership.isLeave() && it.senderId == userCredentials.userId
        }
    }?.map { it.key } ?: emptyList()

    private fun roomInvite(entry: Map.Entry<RoomId, ApiSyncRoomInvite>, userCredentials: UserCredentials): RoomInvite {
        val memberEvents = entry.value.state.events.filterIsInstance<ApiStrippedEvent.RoomMember>()
        val invitee = memberEvents.first { it.content.membership?.isInvite() ?: false }
        val from = memberEvents.first { it.sender == invitee.sender }
        return RoomInvite(
            RoomMember(from.sender, from.content.displayName, from.content.avatarUrl?.convertMxUrToUrl(userCredentials.homeServer)?.let { AvatarUrl(it) }),
            roomId = entry.key,
            inviteMeta = when (invitee.content.isDirect) {
                true -> InviteMeta.DirectMessage
                null, false -> InviteMeta.Room(entry.value.state.events.filterIsInstance<ApiStrippedEvent.RoomName>().firstOrNull()?.content?.name)
            },
        )
    }
}

private fun Map<RoomId, ApiSyncRoom>.keepRoomsWithChanges() = this.filter {
    it.value.state.stateEvents.isNotEmpty() ||
            it.value.timeline.apiTimelineEvents.isNotEmpty() ||
            it.value.accountData?.events?.isNotEmpty() == true ||
            it.value.ephemeral?.events?.isNotEmpty() == true
}

private fun SideEffectResult.roomsToRefresh(alreadyHandledRooms: Set<RoomId>) = this.roomsWithNewKeys.filterNot { alreadyHandledRooms.contains(it) }

private fun ApiSyncResponse.directMessages() = this.accountData?.events?.filterIsInstance<ApiAccountEvent.Direct>()?.firstOrNull()?.let {
    it.content.entries.fold(mutableMapOf<RoomId, UserId>()) { acc, current ->
        current.value.forEach { roomId -> acc[roomId] = current.key }
        acc
    }
} ?: emptyMap()
