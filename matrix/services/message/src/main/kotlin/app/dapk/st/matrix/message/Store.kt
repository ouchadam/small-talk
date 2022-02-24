package app.dapk.st.matrix.message

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import kotlinx.coroutines.flow.Flow

interface LocalEchoStore {

    suspend fun preload()
    suspend fun messageTransaction(message: MessageService.Message, action: suspend () -> EventId)
    fun observeLocalEchos(roomId: RoomId): Flow<List<MessageService.LocalEcho>>
    fun observeLocalEchos(): Flow<Map<RoomId, List<MessageService.LocalEcho>>>
    fun markSending(message: MessageService.Message)
}
