package app.dapk.st.engine

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.JsonString
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface ChatEngine : TaskRunner {

    fun directory(): Flow<DirectoryState>
    fun invites(): Flow<InviteState>
    fun messages(roomId: RoomId, disableReadReceipts: Boolean): Flow<MessengerPageState>

    fun notificationsMessages(): Flow<UnreadNotifications>
    fun notificationsInvites(): Flow<InviteNotification>

    suspend fun login(request: LoginRequest): LoginResult

    suspend fun me(forceRefresh: Boolean): Me

    suspend fun InputStream.importRoomKeys(password: String): Flow<ImportResult>

    suspend fun send(message: SendMessage, room: RoomOverview)

    suspend fun registerPushToken(token: String, gatewayUrl: String)

    suspend fun joinRoom(roomId: RoomId)

    suspend fun rejectJoinRoom(roomId: RoomId)

    suspend fun findMembersSummary(roomId: RoomId): List<RoomMember>

    fun mediaDecrypter(): MediaDecrypter

    fun pushHandler(): PushHandler

}

interface TaskRunner {

    suspend fun runTask(task: ChatEngineTask): TaskResult

    sealed interface TaskResult {
        object Success : TaskResult
        data class Failure(val canRetry: Boolean) : TaskResult
    }

}


data class ChatEngineTask(val type: String, val jsonPayload: String)

interface MediaDecrypter {

    fun decrypt(input: InputStream, k: String, iv: String): Collector

    fun interface Collector {
        fun collect(partial: (ByteArray) -> Unit)
    }

}

interface PushHandler {
    fun onNewToken(payload: JsonString)
    fun onMessageReceived(eventId: EventId?, roomId: RoomId?)
}

typealias UnreadNotifications = Pair<Map<RoomOverview, List<RoomEvent>>, NotificationDiff>

data class NotificationDiff(
    val unchanged: Map<RoomId, List<EventId>>,
    val changedOrNew: Map<RoomId, List<EventId>>,
    val removed: Map<RoomId, List<EventId>>,
    val newRooms: Set<RoomId>
)

data class InviteNotification(
    val content: String,
    val roomId: RoomId
)