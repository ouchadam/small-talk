package app.dapk.st.engine

import app.dapk.st.core.Base64
import app.dapk.st.core.BuildMeta
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.JobBag
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.MatrixTaskRunner
import app.dapk.st.matrix.auth.DeviceDisplayNameGenerator
import app.dapk.st.matrix.auth.authService
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.crypto.MatrixMediaDecrypter
import app.dapk.st.matrix.crypto.cryptoService
import app.dapk.st.matrix.device.KnownDeviceStore
import app.dapk.st.matrix.message.BackgroundScheduler
import app.dapk.st.matrix.message.LocalEchoStore
import app.dapk.st.matrix.message.internal.ImageContentReader
import app.dapk.st.matrix.message.messageService
import app.dapk.st.matrix.push.pushService
import app.dapk.st.matrix.room.MemberStore
import app.dapk.st.matrix.room.ProfileStore
import app.dapk.st.matrix.room.profileService
import app.dapk.st.matrix.room.roomService
import app.dapk.st.matrix.sync.*
import app.dapk.st.olm.OlmStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.InputStream
import java.time.Clock

class MatrixEngine internal constructor(
    private val directoryUseCase: Lazy<DirectoryUseCase>,
    private val matrix: Lazy<MatrixClient>,
    private val timelineUseCase: Lazy<ReadMarkingTimeline>,
    private val sendMessageUseCase: Lazy<SendMessageUseCase>,
    private val matrixMediaDecrypter: Lazy<MatrixMediaDecrypter>,
    private val matrixPushHandler: Lazy<MatrixPushHandler>,
    private val inviteUseCase: Lazy<InviteUseCase>,
    private val notificationMessagesUseCase: Lazy<ObserveUnreadNotificationsUseCase>,
    private val notificationInvitesUseCase: Lazy<ObserveInviteNotificationsUseCase>,
) : ChatEngine {

    override fun directory() = directoryUseCase.value.state()
    override fun invites() = inviteUseCase.value.invites()

    override fun messages(roomId: RoomId, disableReadReceipts: Boolean): Flow<MessengerPageState> {
        return timelineUseCase.value.fetch(roomId, isReadReceiptsDisabled = disableReadReceipts)
    }

    override fun notificationsMessages(): Flow<UnreadNotifications> {
        return notificationMessagesUseCase.value.invoke()
    }

    override fun notificationsInvites(): Flow<InviteNotification> {
        return notificationInvitesUseCase.value.invoke()
    }

    override suspend fun login(request: LoginRequest): LoginResult {
        return matrix.value.authService().login(request.engine()).engine()
    }

    override suspend fun me(forceRefresh: Boolean): Me {
        return matrix.value.profileService().me(forceRefresh).engine()
    }

    override suspend fun InputStream.importRoomKeys(password: String): Flow<ImportResult> {
        return with(matrix.value.cryptoService()) {
            importRoomKeys(password).map { it.engine() }.onEach {
                when (it) {
                    is ImportResult.Error,
                    is ImportResult.Update -> {
                        // do nothing
                    }

                    is ImportResult.Success -> matrix.value.syncService().forceManualRefresh(it.roomIds)
                }
            }
        }
    }

    override suspend fun send(message: SendMessage, room: RoomOverview) {
        sendMessageUseCase.value.send(message, room)
    }

    override suspend fun registerPushToken(token: String, gatewayUrl: String) {
        matrix.value.pushService().registerPush(token, gatewayUrl)
    }

    override suspend fun joinRoom(roomId: RoomId) {
        matrix.value.roomService().joinRoom(roomId)
    }

    override suspend fun rejectJoinRoom(roomId: RoomId) {
        matrix.value.roomService().rejectJoinRoom(roomId)
    }

    override suspend fun findMembersSummary(roomId: RoomId) = matrix.value.roomService().findMembersSummary(roomId)

    override fun mediaDecrypter(): MediaDecrypter {
        val mediaDecrypter = matrixMediaDecrypter.value
        return object : MediaDecrypter {
            override fun decrypt(input: InputStream, k: String, iv: String): MediaDecrypter.Collector {
                return MediaDecrypter.Collector {
                    mediaDecrypter.decrypt(input, k, iv).collect(it)
                }
            }
        }
    }

    override fun pushHandler() = matrixPushHandler.value

    override suspend fun muteRoom(roomId: RoomId) = matrix.value.roomService().muteRoom(roomId)

    override suspend fun unmuteRoom(roomId: RoomId) = matrix.value.roomService().unmuteRoom(roomId)

    override suspend fun runTask(task: ChatEngineTask): TaskRunner.TaskResult {
        return when (val result = matrix.value.run(MatrixTaskRunner.MatrixTask(task.type, task.jsonPayload))) {
            is MatrixTaskRunner.TaskResult.Failure -> TaskRunner.TaskResult.Failure(result.canRetry)
            MatrixTaskRunner.TaskResult.Success -> TaskRunner.TaskResult.Success
        }
    }

    class Factory {

        fun create(
            base64: Base64,
            buildMeta: BuildMeta,
            logger: MatrixLogger,
            nameGenerator: DeviceDisplayNameGenerator,
            coroutineDispatchers: CoroutineDispatchers,
            errorTracker: ErrorTracker,
            imageContentReader: ImageContentReader,
            backgroundScheduler: BackgroundScheduler,
            memberStore: MemberStore,
            roomStore: RoomStore,
            profileStore: ProfileStore,
            syncStore: SyncStore,
            overviewStore: OverviewStore,
            filterStore: FilterStore,
            localEchoStore: LocalEchoStore,
            credentialsStore: CredentialsStore,
            knownDeviceStore: KnownDeviceStore,
            olmStore: OlmStore,
        ): ChatEngine {
            val lazyMatrix = lazy {
                MatrixFactory.createMatrix(
                    base64,
                    buildMeta,
                    logger,
                    nameGenerator,
                    coroutineDispatchers,
                    errorTracker,
                    imageContentReader,
                    backgroundScheduler,
                    memberStore,
                    roomStore,
                    profileStore,
                    syncStore,
                    overviewStore,
                    filterStore,
                    localEchoStore,
                    credentialsStore,
                    knownDeviceStore,
                    olmStore
                )
            }
            val directoryUseCase = unsafeLazy {
                val matrix = lazyMatrix.value
                DirectoryUseCase(
                    matrix.syncService(),
                    matrix.messageService(),
                    matrix.roomService(),
                    credentialsStore,
                    roomStore
                )
            }
            val timelineUseCase = unsafeLazy {
                val matrix = lazyMatrix.value
                val mergeWithLocalEchosUseCase = MergeWithLocalEchosUseCaseImpl(LocalEchoMapper(MetaMapper()))
                val timeline = TimelineUseCaseImpl(matrix.syncService(), matrix.messageService(), matrix.roomService(), mergeWithLocalEchosUseCase)
                ReadMarkingTimeline(roomStore, credentialsStore, timeline, matrix.roomService())
            }

            val sendMessageUseCase = unsafeLazy {
                val matrix = lazyMatrix.value
                SendMessageUseCase(matrix.messageService(), LocalIdFactory(), imageContentReader, Clock.systemUTC())
            }

            val mediaDecrypter = unsafeLazy { MatrixMediaDecrypter(base64) }
            val pushHandler = unsafeLazy {
                MatrixPushHandler(
                    backgroundScheduler,
                    credentialsStore,
                    lazyMatrix.value.syncService(),
                    roomStore,
                    coroutineDispatchers,
                    JobBag(),
                )
            }

            val invitesUseCase = unsafeLazy { InviteUseCase(lazyMatrix.value.syncService()) }

            return MatrixEngine(
                directoryUseCase,
                lazyMatrix,
                timelineUseCase,
                sendMessageUseCase,
                mediaDecrypter,
                pushHandler,
                invitesUseCase,
                unsafeLazy { ObserveUnreadNotificationsUseCaseImpl(roomStore) },
                unsafeLazy { ObserveInviteNotificationsUseCaseImpl(overviewStore) },
            )
        }

    }

}

private fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(mode = LazyThreadSafetyMode.NONE, initializer = initializer)
