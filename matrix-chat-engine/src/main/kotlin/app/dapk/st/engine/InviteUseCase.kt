package app.dapk.st.engine

import app.dapk.st.matrix.sync.SyncService
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class InviteUseCase(
    private val syncService: SyncService
) {

    fun invites() = invitesDatasource()

    private fun invitesDatasource() = combine(
        syncService.startSyncing().map { false }.onStart { emit(true) },
        syncService.invites().map { it.map { it.engine() } }
    ) { isFirstLoad, invites ->
        when {
            isFirstLoad && invites.isEmpty() -> null
            else -> invites
        }
    }.filterNotNull()

}