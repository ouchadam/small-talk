package app.dapk.st.matrix.sync

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.SyncToken
import kotlinx.coroutines.flow.Flow

interface RoomStore : MuteableStore {

    suspend fun persist(roomId: RoomId, events: List<RoomEvent>)
    suspend fun remove(rooms: List<RoomId>)
    suspend fun remove(eventId: EventId)
    suspend fun retrieve(roomId: RoomId): RoomState?
    fun latest(roomId: RoomId): Flow<RoomState>
    suspend fun insertUnread(roomId: RoomId, eventIds: List<EventId>)
    suspend fun markRead(roomId: RoomId)
    fun observeUnread(): Flow<Map<RoomOverview, List<RoomEvent>>>
    fun observeUnreadCountById(): Flow<Map<RoomId, Int>>
    fun observeNotMutedUnread(): Flow<Map<RoomOverview, List<RoomEvent>>>
    fun observeEvent(eventId: EventId): Flow<EventId>
    suspend fun findEvent(eventId: EventId): RoomEvent?

}

interface MuteableStore {
    suspend fun mute(roomId: RoomId)
    suspend fun unmute(roomId: RoomId)
    suspend fun isMuted(roomId: RoomId): Boolean
    fun observeMuted(): Flow<Set<RoomId>>
}

interface FilterStore {

    suspend fun store(key: String, filterId: String)
    suspend fun read(key: String): String?
}

interface OverviewStore {

    suspend fun removeRooms(roomsToRemove: List<RoomId>)
    suspend fun persistInvites(invite: List<RoomInvite>)
    suspend fun persist(overviewState: OverviewState)

    suspend fun retrieve(): OverviewState?

    fun latest(): Flow<OverviewState>
    fun latestInvites(): Flow<List<RoomInvite>>
    suspend fun removeInvites(map: List<RoomId>)
}

interface SyncStore {

    suspend fun store(key: SyncKey, syncToken: SyncToken)
    suspend fun read(key: SyncKey): SyncToken?
    suspend fun remove(key: SyncKey)

    sealed interface SyncKey {

        val value: String

        object Overview : SyncKey {

            override val value = "overview-sync-token"
        }

        data class Room(val roomId: RoomId) : SyncKey {

            override val value = "room-sync-token-${roomId.value}"
        }
    }
}