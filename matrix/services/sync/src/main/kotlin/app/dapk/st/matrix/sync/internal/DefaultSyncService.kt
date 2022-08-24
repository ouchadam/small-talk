package app.dapk.st.matrix.sync.internal

import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.withIoContext
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.sync.*
import app.dapk.st.matrix.sync.internal.filter.FilterUseCase
import app.dapk.st.matrix.sync.internal.overview.ReducedSyncFilterUseCase
import app.dapk.st.matrix.sync.internal.room.MessageDecrypter
import app.dapk.st.matrix.sync.internal.room.RoomEventsDecrypter
import app.dapk.st.matrix.sync.internal.room.SyncEventDecrypter
import app.dapk.st.matrix.sync.internal.room.SyncSideEffects
import app.dapk.st.matrix.sync.internal.sync.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger

private val syncSubscriptionCount = AtomicInteger()

internal class DefaultSyncService(
    httpClient: MatrixHttpClient,
    syncStore: SyncStore,
    private val overviewStore: OverviewStore,
    private val roomStore: RoomStore,
    filterStore: FilterStore,
    messageDecrypter: MessageDecrypter,
    keySharer: KeySharer,
    verificationHandler: VerificationHandler,
    deviceNotifier: DeviceNotifier,
    json: Json,
    oneTimeKeyProducer: MaybeCreateMoreKeys,
    scope: CoroutineScope,
    private val credentialsStore: CredentialsStore,
    roomMembersService: RoomMembersService,
    logger: MatrixLogger,
    errorTracker: ErrorTracker,
    private val coroutineDispatchers: CoroutineDispatchers,
    syncConfig: SyncConfig,
) : SyncService {

    private val syncEventsFlow = MutableStateFlow<List<SyncService.SyncEvent>>(emptyList())

    private val roomDataSource by lazy { RoomDataSource(roomStore, logger) }
    private val eventDecrypter by lazy { SyncEventDecrypter(messageDecrypter, json, logger) }
    private val roomEventsDecrypter by lazy { RoomEventsDecrypter(messageDecrypter, json, logger) }
    private val roomRefresher by lazy { RoomRefresher(roomDataSource, roomEventsDecrypter, logger) }

    private val sync2 by lazy {
        val roomDataSource = RoomDataSource(roomStore, logger)
        val syncReducer = SyncReducer(
            RoomProcessor(
                roomMembersService,
                roomDataSource,
                TimelineEventsProcessor(
                    RoomEventCreator(roomMembersService, errorTracker, RoomEventFactory(roomMembersService)),
                    roomEventsDecrypter,
                    eventDecrypter,
                    EventLookupUseCase(roomStore)
                ),
                RoomOverviewProcessor(roomMembersService),
                UnreadEventsProcessor(roomStore, logger),
                EphemeralEventsUseCase(roomMembersService, syncEventsFlow),
            ),
            roomRefresher,
            roomDataSource,
            logger,
            errorTracker,
            coroutineDispatchers,
        )
        SyncUseCase(
            overviewStore,
            SideEffectFlowIterator(logger),
            SyncSideEffects(keySharer, verificationHandler, deviceNotifier, messageDecrypter, json, oneTimeKeyProducer, logger),
            httpClient,
            syncStore,
            syncReducer,
            credentialsStore,
            logger,
            ReducedSyncFilterUseCase(FilterUseCase(httpClient, filterStore)),
            syncConfig,
        )
    }

    private val syncFlow by lazy {
        sync2.sync().let {
            if (syncConfig.allowSharedFlows) {
                it.shareIn(scope, SharingStarted.WhileSubscribed(5000))
            } else {
                it
            }
        }
            .onStart {
                val subscriptions = syncSubscriptionCount.incrementAndGet()
                logger.matrixLog(MatrixLogTag.SYNC, "flow onStart - count: $subscriptions")
            }
            .onCompletion {
                val subscriptions = syncSubscriptionCount.decrementAndGet()
                logger.matrixLog(MatrixLogTag.SYNC, "flow onCompletion - count: $subscriptions")
            }
    }

    override fun startSyncing() = syncFlow
    override fun invites() = overviewStore.latestInvites()
    override fun overview() = overviewStore.latest()
    override fun room(roomId: RoomId) = roomStore.latest(roomId)
    override fun events() = syncEventsFlow
    override suspend fun observeEvent(eventId: EventId) = roomStore.observeEvent(eventId)
    override suspend fun forceManualRefresh(roomIds: List<RoomId>) {
        coroutineDispatchers.withIoContext {
            roomIds.map {
                async {
                    roomRefresher.refreshRoomContent(it, credentialsStore.credentials()!!)?.also {
                        overviewStore.persist(listOf(it.roomOverview))
                    }
                }
            }.awaitAll()
        }
    }
}
