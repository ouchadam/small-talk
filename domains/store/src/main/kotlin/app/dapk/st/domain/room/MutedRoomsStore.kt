package app.dapk.st.domain.room

import app.dapk.st.core.Preferences
import app.dapk.st.core.append
import app.dapk.st.core.removeFromSet
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.MuteableStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart

private const val KEY_MUTE = "mute"

internal class MutedStorePersistence(
    private val preferences: Preferences
) : MuteableStore {

    private val allMutedFlow = MutableSharedFlow<Set<RoomId>>(replay = 1)

    override suspend fun mute(roomId: RoomId) {
        preferences.append(KEY_MUTE, roomId.value).notifyChange()
    }

    override suspend fun unmute(roomId: RoomId) {
        preferences.removeFromSet(KEY_MUTE, roomId.value).notifyChange()
    }

    private suspend fun Set<String>.notifyChange() = allMutedFlow.emit(this.map { RoomId(it) }.toSet())

    override suspend fun isMuted(roomId: RoomId) = allMutedFlow.firstOrNull()?.contains(roomId) ?: false

    override fun observeMuted(): Flow<Set<RoomId>> = allMutedFlow.onStart { emit(readAll()) }

    private suspend fun readAll(): Set<RoomId> {
        return preferences.readStrings(KEY_MUTE)?.map { RoomId(it) }?.toSet() ?: emptySet()
    }

}