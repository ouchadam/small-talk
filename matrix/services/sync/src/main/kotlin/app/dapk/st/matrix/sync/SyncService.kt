package app.dapk.st.matrix.sync

import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.ServiceDepFactory
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.internal.DefaultSyncService
import app.dapk.st.matrix.sync.internal.SideEffectFlowIterator
import app.dapk.st.matrix.sync.internal.request.*
import app.dapk.st.matrix.sync.internal.room.MessageDecrypter
import app.dapk.st.matrix.sync.internal.room.MissingMessageDecrypter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

private val SERVICE_KEY = SyncService::class

interface SyncService : MatrixService {

    suspend fun invites(): Flow<InviteState>
    suspend fun overview(): Flow<OverviewState>
    suspend fun room(roomId: RoomId): Flow<RoomState>
    suspend fun startSyncing(): Flow<Unit>
    suspend fun events(): Flow<List<SyncEvent>>
    suspend fun observeEvent(eventId: EventId): Flow<EventId>
    suspend fun forceManualRefresh(roomIds: List<RoomId>)

    @JvmInline
    value class FilterId(val value: String)

    sealed interface SyncEvent {
        data class Typing(val roomId: RoomId, val members: List<RoomMember>) : SyncEvent
    }
}

fun MatrixServiceInstaller.installSyncService(
    credentialsStore: CredentialsStore,
    overviewStore: OverviewStore,
    roomStore: RoomStore,
    syncStore: SyncStore,
    filterStore: FilterStore,
    deviceNotifier: ServiceDepFactory<DeviceNotifier>,
    messageDecrypter: ServiceDepFactory<MessageDecrypter> = ServiceDepFactory { MissingMessageDecrypter },
    keySharer: ServiceDepFactory<KeySharer> = ServiceDepFactory { NoOpKeySharer },
    verificationHandler: ServiceDepFactory<VerificationHandler> = ServiceDepFactory { NoOpVerificationHandler },
    oneTimeKeyProducer: ServiceDepFactory<MaybeCreateMoreKeys>,
    roomMembersService: ServiceDepFactory<RoomMembersService>,
    errorTracker: ErrorTracker,
    coroutineDispatchers: CoroutineDispatchers,
    syncConfig: SyncConfig = SyncConfig(),
) {
    this.serializers {
        polymorphicDefault(ApiTimelineEvent::class) {
            ApiTimelineEvent.Ignored.serializer()
        }
        polymorphicDefault(ApiToDeviceEvent::class) {
            ApiToDeviceEvent.Ignored.serializer()
        }
        polymorphicDefault(ApiAccountEvent::class) {
            ApiAccountEvent.Ignored.serializer()
        }
        polymorphicDefault(ApiEphemeralEvent::class) {
            ApiEphemeralEvent.Ignored.serializer()
        }
        polymorphicDefault(ApiStrippedEvent::class) {
            ApiStrippedEvent.Ignored.serializer()
        }
        polymorphicDefault(DecryptedContent::class) {
            DecryptedContent.Ignored.serializer()
        }
    }

    this.install { (httpClient, json, services, logger) ->
        SERVICE_KEY to DefaultSyncService(
            httpClient = httpClient,
            syncStore = syncStore,
            overviewStore = overviewStore,
            roomStore = roomStore,
            filterStore = filterStore,
            messageDecrypter = messageDecrypter.create(services),
            keySharer = keySharer.create(services),
            verificationHandler = verificationHandler.create(services),
            deviceNotifier = deviceNotifier.create(services),
            json = json,
            oneTimeKeyProducer = oneTimeKeyProducer.create(services),
            scope = CoroutineScope(coroutineDispatchers.io),
            credentialsStore = credentialsStore,
            roomMembersService = roomMembersService.create(services),
            logger = logger,
            errorTracker = errorTracker,
            coroutineDispatchers = coroutineDispatchers,
            syncConfig = syncConfig,
        )
    }
}

fun MatrixClient.syncService(): SyncService = this.getService(key = SERVICE_KEY)

fun interface KeySharer {
    suspend fun share(keys: List<SharedRoomKey>)
}

fun interface VerificationHandler {
    suspend fun handle(apiVerificationEvent: ApiToDeviceEvent.ApiVerificationEvent)
}

internal object NoOpVerificationHandler : VerificationHandler {
    override suspend fun handle(apiVerificationEvent: ApiToDeviceEvent.ApiVerificationEvent) {
        // do nothing
    }
}

fun interface MaybeCreateMoreKeys {
    suspend fun onServerKeyCount(count: ServerKeyCount)
}


fun interface DeviceNotifier {
    suspend fun notifyChanges(userId: List<UserId>, syncToken: SyncToken?)
}

internal object NoOpKeySharer : KeySharer {
    override suspend fun share(keys: List<SharedRoomKey>) {
        // do nothing
    }
}

interface RoomMembersService {
    suspend fun find(roomId: RoomId, userIds: List<UserId>): List<RoomMember>
    suspend fun insert(roomId: RoomId, members: List<RoomMember>)
}

suspend fun RoomMembersService.find(roomId: RoomId, userId: UserId): RoomMember? {
    return this.find(roomId, listOf(userId)).firstOrNull()
}

data class SyncConfig(
    val loopTimeout: Long = 30_000L,
    val allowSharedFlows: Boolean = true
)