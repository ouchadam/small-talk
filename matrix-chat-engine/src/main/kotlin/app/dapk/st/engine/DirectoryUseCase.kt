package app.dapk.st.engine

import app.dapk.st.core.extensions.combine
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.message.MessageService
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
    private val credentialsStore: CredentialsStore,
    private val roomStore: RoomStore,
    private val mergeLocalEchosUseCase: DirectoryMergeWithLocalEchosUseCase,
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
                mergeLocalEchosUseCase.invoke(overviewState, userId, localEchos).map { roomOverview ->
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
}
