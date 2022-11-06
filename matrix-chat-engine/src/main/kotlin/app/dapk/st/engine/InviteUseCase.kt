package app.dapk.st.engine

import app.dapk.st.matrix.sync.SyncService
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class InviteUseCase(
    private val syncService: SyncService
) {

    fun invites() = invitesDatasource()

    private fun invitesDatasource() = combine(
        syncService.startSyncing(),
        syncService.invites().map { it.map { it.engine() } }
    ) { _, invites -> invites }

}