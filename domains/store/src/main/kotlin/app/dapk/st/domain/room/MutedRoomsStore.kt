package app.dapk.st.domain.room

import app.dapk.st.core.Preferences
import app.dapk.st.core.append
import app.dapk.st.core.removeFromSet
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.MuteableStore

private const val KEY_MUTE = "mute"

internal class MutedStorePersistence(
    private val preferences: Preferences
) : MuteableStore {

    override suspend fun mute(roomId: RoomId) {
        preferences.append(KEY_MUTE, roomId.value)
    }

    override suspend fun unmute(roomId: RoomId) {
        preferences.removeFromSet(KEY_MUTE, roomId.value)
    }

    override suspend fun isMuted(roomId: RoomId): Boolean {
        val allMuted = allMuted()
        return allMuted.contains(roomId)
    }

    override suspend fun allMuted(): Set<RoomId> {
        return preferences.readStrings(KEY_MUTE)?.map { RoomId(it) }?.toSet() ?: emptySet()
    }

}