package app.dapk.st.directory

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.*
import app.dapk.st.matrix.sync.SyncService.SyncEvent.Typing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@JvmInline
value class UnreadCount(val value: Int)

typealias DirectoryState = List<RoomFoo>

data class RoomFoo(
    val overview: RoomOverview,
    val unreadCount: UnreadCount,
    val typing: Typing?
)

class DirectoryUseCase(
    private val syncService: SyncService,
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val credentialsStore: CredentialsStore,
    private val roomStore: RoomStore,
) {

    fun state(): Flow<DirectoryState> {
        return combine(
            syncService.startSyncing().map { credentialsStore.credentials()!!.userId },
            syncService.overview(),
            messageService.localEchos(),
            roomStore.observeUnreadCountById(),
            syncService.events()
        ) { userId, overviewState, localEchos, unread, events ->
            overviewState.mergeWithLocalEchos(localEchos, userId).map { roomOverview ->
                RoomFoo(
                    overview = roomOverview,
                    unreadCount = UnreadCount(unread[roomOverview.roomId] ?: 0),
                    typing = events.filterIsInstance<Typing>().firstOrNull { it.roomId == roomOverview.roomId }
                )
            }
        }
    }

    private suspend fun OverviewState.mergeWithLocalEchos(localEchos: Map<RoomId, List<MessageService.LocalEcho>>, userId: UserId): OverviewState {
        return when {
            localEchos.isEmpty() -> this
            else -> this.map {
                when (val roomEchos = localEchos[it.roomId]) {
                    null -> it
                    else -> it.mergeWithLocalEchos(
                        member = roomService.findMember(it.roomId, userId) ?: RoomMember(
                            userId,
                            null,
                            avatarUrl = null,
                        ),
                        echos = roomEchos,
                    )
                }
            }
        }
    }

    private fun RoomOverview.mergeWithLocalEchos(member: RoomMember, echos: List<MessageService.LocalEcho>): RoomOverview {
        val latestEcho = echos.maxByOrNull { it.timestampUtc }
        return if (latestEcho != null && latestEcho.timestampUtc > (this.lastMessage?.utcTimestamp ?: 0)) {
            this.copy(
                lastMessage = LastMessage(
                    content = when (val message = latestEcho.message) {
                        is MessageService.Message.TextMessage -> message.content.body
                        is MessageService.Message.ImageMessage -> "\uD83D\uDCF7"
                    },
                    utcTimestamp = latestEcho.timestampUtc,
                    author = member,
                )
            )
        } else {
            this
        }
    }

}
