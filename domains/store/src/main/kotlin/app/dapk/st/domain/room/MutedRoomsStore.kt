package app.dapk.st.domain.room

import app.dapk.st.core.Preferences
import app.dapk.st.core.append
import app.dapk.st.core.removeFromSet
import app.dapk.st.matrix.common.RoomId

private const val KEY_MUTE = "mute"

interface MutedRoomsStore {
    suspend fun mute(roomId: RoomId)
    suspend fun unmute(roomId: RoomId)
    suspend fun isMuted(roomId: RoomId): Boolean
    suspend fun allMuted(): Set<RoomId>
}

class MutedRoomsStorePersistence(
    private val preferences: Preferences
) : MutedRoomsStore {

    override suspend fun mute(roomId: RoomId) {
        preferences.append(KEY_MUTE, roomId.value)
    }

    override suspend fun unmute(roomId: RoomId) {
        preferences.removeFromSet(KEY_MUTE, roomId.value)
    }

    override suspend fun isMuted(roomId: RoomId): Boolean {
        val allMuted = allMuted()
        println("??? isMuted - $roomId")
        println("??? all - $allMuted")
        return allMuted.contains(roomId)
    }

    override suspend fun allMuted(): Set<RoomId> {
        return preferences.readStrings(KEY_MUTE)?.map { RoomId(it) }?.toSet() ?: emptySet()
    }

}