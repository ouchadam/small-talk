package app.dapk.st.matrix.sync

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.SyncToken
import kotlinx.coroutines.flow.Flow

interface RoomStore {

    suspend fun persist(roomId: RoomId, state: RoomState)
    suspend fun retrieve(roomId: RoomId): RoomState?
    fun latest(roomId: RoomId): Flow<RoomState>
    suspend fun insertUnread(roomId: RoomId, eventIds: List<EventId>)
    suspend fun markRead(roomId: RoomId)
    suspend fun observeUnread(): Flow<Map<RoomOverview, List<RoomEvent>>>
    fun observeUnreadCountById(): Flow<Map<RoomId, Int>>
    suspend fun observeEvent(eventId: EventId): Flow<EventId>
    suspend fun findEvent(eventId: EventId): RoomEvent?

}

interface FilterStore {

    suspend fun store(key: String, filterId: String)

    suspend fun read(key: String): String?
}

interface OverviewStore {

    suspend fun persistInvites(invite: List<RoomInvite>)
    suspend fun persist(overviewState: OverviewState)

    suspend fun retrieve(): OverviewState?

    fun latest(): Flow<OverviewState>
    fun latestInvites(): Flow<List<RoomInvite>>
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