package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.common.MatrixLogTag.SYNC
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.sync.*
import app.dapk.st.matrix.sync.internal.SideEffectFlowIterator
import app.dapk.st.matrix.sync.internal.overview.ReducedSyncFilterUseCase
import app.dapk.st.matrix.sync.internal.request.syncRequest
import app.dapk.st.matrix.sync.internal.room.SyncSideEffects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow

internal class SyncUseCase(
    private val persistence: OverviewStore,
    private val flowIterator: SideEffectFlowIterator,
    private val syncSideEffects: SyncSideEffects,
    private val client: MatrixHttpClient,
    private val syncStore: SyncStore,
    private val syncReducer: SyncReducer,
    private val credentialsStore: CredentialsStore,
    private val logger: MatrixLogger,
    private val filterUseCase: ReducedSyncFilterUseCase,
    private val syncConfig: SyncConfig,
) {

    fun sync(): Flow<Unit> {
        return flow {
            val credentials = credentialsStore.credentials()!!
            val filterId = filterUseCase.reducedFilter(credentials.userId)
            with(flowIterator) {
                loop<OverviewState>(
                    initial = null,
                    onPost = { emit(Unit) },
                    onIteration = { onEachSyncIteration(filterId, credentials, previousState = it) }
                )
            }
        }.cancellable()
    }

    private suspend fun onEachSyncIteration(filterId: SyncService.FilterId, credentials: UserCredentials, previousState: OverviewState?): OverviewState? {
        val syncToken = syncStore.read(key = SyncStore.SyncKey.Overview)
        val response = doSyncRequest(filterId, syncToken)
        return if (credentialsStore.isSignedIn()) {
            logger.logP("sync processing") {
                syncStore.store(key = SyncStore.SyncKey.Overview, syncToken = response.nextBatch)
                val sideEffects = logger.logP("side effects processing") {
                    syncSideEffects.blockingSideEffects(credentials.userId, response, syncToken)
                }

                val isInitialSync = syncToken == null
                val nextState = logger.logP("reducing") { syncReducer.reduce(isInitialSync, sideEffects, response, credentials) }
                val overview = nextState.roomState.map { it.roomOverview }

                if (nextState.roomsLeft.isNotEmpty()) {
                    persistence.removeRooms(nextState.roomsLeft)
                }
                if (nextState.invites.isNotEmpty()) {
                    persistence.persistInvites(nextState.invites)
                }
                if (nextState.newRoomsJoined.isNotEmpty()) {
                    persistence.removeInvites(nextState.newRoomsJoined)
                }

                when {
                    previousState == overview -> previousState.also { logger.matrixLog(SYNC, "no changes, not persisting new state") }
                    overview.isNotEmpty() -> overview.also { persistence.persist(overview) }
                    else -> previousState.also { logger.matrixLog(SYNC, "nothing to do") }
                }
            }
        } else {
            logger.matrixLog(SYNC, "sync processing skipped due to being signed out")
            null
        }
    }

    private suspend fun doSyncRequest(filterId: SyncService.FilterId, syncToken: SyncToken?) = logger.logP("sync api") {
        client.execute(
            syncRequest(
                lastSyncToken = syncToken,
                filterId = filterId,
                timeoutMs = syncConfig.loopTimeout,
            )
        )
    }

}
