package app.dapk.st.engine

import app.dapk.st.core.extensions.combine
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.matrix.sync.SyncService.SyncEvent.Typing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class DirectoryUseCase(
    private val syncService: SyncService,
    private val messageService: MessageService,
    private val roomService: RoomService,
    private val credentialsStore: CredentialsStore,
    private val roomStore: RoomStore,
) {

    fun state(): Flow<DirectoryState> {
        return flow { emit(credentialsStore.credentials()!!.userId) }.flatMapConcat { userId ->
            combine(
                syncService.startSyncing(),
                syncService.overview().map { it.map { it.engine() } },
                messageService.localEchos(),
                roomStore.observeUnreadCountById(),
                syncService.events(),
                roomStore.observeMuted(),
            ) { _, overviewState, localEchos, unread, events, muted ->
                overviewState.mergeWithLocalEchos(localEchos, userId).map { roomOverview ->
                    DirectoryItem(
                        overview = roomOverview,
                        unreadCount = UnreadCount(unread[roomOverview.roomId] ?: 0),
                        typing = events.filterIsInstance<Typing>().firstOrNull { it.roomId == roomOverview.roomId }?.engine(),
                        isMuted = muted.contains(roomOverview.roomId),
                    )
                }
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
                lastMessage = RoomOverview.LastMessage(
                    content = when (val message = latestEcho.message) {
                        is MessageService.Message.TextMessage -> message.content.body.asString()
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
