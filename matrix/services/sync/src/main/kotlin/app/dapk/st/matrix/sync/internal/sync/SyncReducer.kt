package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.core.CoroutineDispatchers
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
    private val logger: MatrixLogger,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    data class ReducerResult(
        val roomState: List<RoomState>,
        val invites: List<RoomInvite>
    )

    suspend fun reduce(isInitialSync: Boolean, sideEffects: SideEffectResult, response: ApiSyncResponse, userCredentials: UserCredentials): ReducerResult {
        val directMessages = response.directMessages()

        val invites = response.rooms?.invite?.map { roomInvite(it, userCredentials) } ?: emptyList()
        val apiUpdatedRooms = response.rooms?.join?.keepRoomsWithChanges()
        val apiRoomsToProcess = apiUpdatedRooms?.map { (roomId, apiRoom) ->
            logger.matrixLog(SYNC, "reducing: $roomId")
            coroutineDispatchers.withIoContextAsync {
                roomProcessor.processRoom(
                    roomToProcess = RoomToProcess(
                        roomId = roomId,
                        apiSyncRoom = apiRoom,
                        directMessage = directMessages[roomId],
                        userCredentials = userCredentials,
                    ),
                    isInitialSync = isInitialSync
                )
            }
        } ?: emptyList()

        val roomsWithSideEffects = sideEffects.roomsToRefresh(alreadyHandledRooms = apiUpdatedRooms?.keys ?: emptySet()).map { roomId ->
            coroutineDispatchers.withIoContextAsync {
                roomRefresher.refreshRoomContent(roomId)
            }
        }

        return ReducerResult((apiRoomsToProcess + roomsWithSideEffects).awaitAll().filterNotNull(), invites)
    }

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
