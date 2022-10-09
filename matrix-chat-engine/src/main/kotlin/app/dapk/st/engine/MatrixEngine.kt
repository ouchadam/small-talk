package app.dapk.st.engine

import app.dapk.st.core.Base64
import app.dapk.st.core.BuildMeta
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.SingletonFlows
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.auth.DeviceDisplayNameGenerator
import app.dapk.st.matrix.auth.authService
import app.dapk.st.matrix.auth.installAuthService
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.RoomMembersProvider
import app.dapk.st.matrix.crypto.Verification
import app.dapk.st.matrix.crypto.cryptoService
import app.dapk.st.matrix.crypto.installCryptoService
import app.dapk.st.matrix.device.KnownDeviceStore
import app.dapk.st.matrix.device.deviceService
import app.dapk.st.matrix.device.installEncryptionService
import app.dapk.st.matrix.http.ktor.KtorMatrixHttpClientFactory
import app.dapk.st.matrix.message.*
import app.dapk.st.matrix.message.internal.ImageContentReader
import app.dapk.st.matrix.push.installPushService
import app.dapk.st.matrix.room.*
import app.dapk.st.matrix.sync.*
import app.dapk.st.matrix.sync.internal.request.ApiToDeviceEvent
import app.dapk.st.matrix.sync.internal.room.MessageDecrypter
import app.dapk.st.olm.DeviceKeyFactory
import app.dapk.st.olm.OlmStore
import app.dapk.st.olm.OlmWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.time.Clock

class MatrixEngine internal constructor(
    private val directoryUseCase: Lazy<DirectoryUseCase>,
    private val matrix: Lazy<MatrixClient>,
    private val timelineUseCase: Lazy<ReadMarkingTimeline>,
    private val sendMessageUseCase: Lazy<SendMessageUseCase>,
) : ChatEngine {

    override fun directory() = directoryUseCase.value.state()
    override fun invites(): Flow<InviteState> {
        return matrix.value.syncService().invites().map { it.map { it.engine() } }
    }

    override suspend fun messages(roomId: RoomId, disableReadReceipts: Boolean): Flow<MessengerState> {
        return timelineUseCase.value.foo(roomId, isReadReceiptsDisabled = disableReadReceipts)
    }

    override suspend fun login(request: LoginRequest): LoginResult {
        return matrix.value.authService().login(request.engine()).engine()
    }

    override suspend fun me(forceRefresh: Boolean): Me {
        return matrix.value.profileService().me(forceRefresh).engine()
    }

    override suspend fun refresh(roomIds: List<RoomId>) {
        matrix.value.syncService().forceManualRefresh(roomIds)

    }

    override suspend fun InputStream.importRoomKeys(password: String): Flow<ImportResult> {
        return with(matrix.value.cryptoService()) {
            importRoomKeys(password).map { it.engine() }
        }
    }

    override suspend fun send(message: SendMessage, room: RoomOverview) {
        sendMessageUseCase.value.send(message, room)
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

            return MatrixEngine(directoryUseCase, lazyMatrix, timelineUseCase, sendMessageUseCase)

        }

    }

}


object MatrixFactory {

    fun createMatrix(
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
    ) = MatrixClient(
        KtorMatrixHttpClientFactory(
            credentialsStore,
            includeLogging = buildMeta.isDebug,
        ),
        logger
    ).also {
        it.install {
            installAuthService(credentialsStore, nameGenerator)
            installEncryptionService(knownDeviceStore)

            val singletonFlows = SingletonFlows(coroutineDispatchers)
            val olm = OlmWrapper(
                olmStore = olmStore,
                singletonFlows = singletonFlows,
                jsonCanonicalizer = JsonCanonicalizer(),
                deviceKeyFactory = DeviceKeyFactory(JsonCanonicalizer()),
                errorTracker = errorTracker,
                logger = logger,
                clock = Clock.systemUTC(),
                coroutineDispatchers = coroutineDispatchers,
            )
            installCryptoService(
                credentialsStore,
                olm,
                roomMembersProvider = { services ->
                    RoomMembersProvider {
                        services.roomService().joinedMembers(it).map { it.userId }
                    }
                },
                base64 = base64,
                coroutineDispatchers = coroutineDispatchers,
            )
            installMessageService(
                localEchoStore,
                backgroundScheduler,
                imageContentReader,
                messageEncrypter = {
                    val cryptoService = it.cryptoService()
                    MessageEncrypter { message ->
                        val result = cryptoService.encrypt(
                            roomId = message.roomId,
                            credentials = credentialsStore.credentials()!!,
                            messageJson = message.contents,
                        )

                        MessageEncrypter.EncryptedMessagePayload(
                            result.algorithmName,
                            result.senderKey,
                            result.cipherText,
                            result.sessionId,
                            result.deviceId,
                        )
                    }
                },
                mediaEncrypter = {
                    val cryptoService = it.cryptoService()
                    MediaEncrypter { input ->
                        val result = cryptoService.encrypt(input)
                        MediaEncrypter.Result(
                            uri = result.uri,
                            contentLength = result.contentLength,
                            algorithm = result.algorithm,
                            ext = result.ext,
                            keyOperations = result.keyOperations,
                            kty = result.kty,
                            k = result.k,
                            iv = result.iv,
                            hashes = result.hashes,
                            v = result.v,
                        )
                    }
                },
            )

            installRoomService(
                memberStore,
                roomMessenger = {
                    val messageService = it.messageService()
                    object : RoomMessenger {
                        override suspend fun enableEncryption(roomId: RoomId) {
                            messageService.sendEventMessage(
                                roomId, MessageService.EventMessage.Encryption(
                                    algorithm = AlgorithmName("m.megolm.v1.aes-sha2")
                                )
                            )
                        }
                    }
                },
                roomInviteRemover = {
                    overviewStore.removeInvites(listOf(it))
                }
            )

            installProfileService(profileStore, singletonFlows, credentialsStore)

            installSyncService(
                credentialsStore,
                overviewStore,
                roomStore,
                syncStore,
                filterStore,
                deviceNotifier = { services ->
                    val encryption = services.deviceService()
                    val crypto = services.cryptoService()
                    DeviceNotifier { userIds, syncToken ->
                        encryption.updateStaleDevices(userIds)
                        crypto.updateOlmSession(userIds, syncToken)
                    }
                },
                messageDecrypter = { serviceProvider ->
                    val cryptoService = serviceProvider.cryptoService()
                    MessageDecrypter {
                        cryptoService.decrypt(it)
                    }
                },
                keySharer = { serviceProvider ->
                    val cryptoService = serviceProvider.cryptoService()
                    KeySharer { sharedRoomKeys ->
                        cryptoService.importRoomKeys(sharedRoomKeys)
                    }
                },
                verificationHandler = { services ->
                    val cryptoService = services.cryptoService()
                    VerificationHandler { apiEvent ->
                        logger.matrixLog(MatrixLogTag.VERIFICATION, "got a verification request $it")
                        cryptoService.onVerificationEvent(
                            when (apiEvent) {
                                is ApiToDeviceEvent.VerificationRequest -> Verification.Event.Requested(
                                    apiEvent.sender,
                                    apiEvent.content.fromDevice,
                                    apiEvent.content.transactionId,
                                    apiEvent.content.methods,
                                    apiEvent.content.timestampPosix,
                                )

                                is ApiToDeviceEvent.VerificationReady -> Verification.Event.Ready(
                                    apiEvent.sender,
                                    apiEvent.content.fromDevice,
                                    apiEvent.content.transactionId,
                                    apiEvent.content.methods,
                                )

                                is ApiToDeviceEvent.VerificationStart -> Verification.Event.Started(
                                    apiEvent.sender,
                                    apiEvent.content.fromDevice,
                                    apiEvent.content.method,
                                    apiEvent.content.protocols,
                                    apiEvent.content.hashes,
                                    apiEvent.content.codes,
                                    apiEvent.content.short,
                                    apiEvent.content.transactionId,
                                )

                                is ApiToDeviceEvent.VerificationCancel -> TODO()
                                is ApiToDeviceEvent.VerificationAccept -> TODO()
                                is ApiToDeviceEvent.VerificationKey -> Verification.Event.Key(
                                    apiEvent.sender,
                                    apiEvent.content.transactionId,
                                    apiEvent.content.key
                                )

                                is ApiToDeviceEvent.VerificationMac -> Verification.Event.Mac(
                                    apiEvent.sender,
                                    apiEvent.content.transactionId,
                                    apiEvent.content.keys,
                                    apiEvent.content.mac,
                                )
                            }
                        )
                    }
                },
                oneTimeKeyProducer = { services ->
                    val cryptoService = services.cryptoService()
                    MaybeCreateMoreKeys {
                        cryptoService.maybeCreateMoreKeys(it)
                    }
                },
                roomMembersService = { services ->
                    val roomService = services.roomService()
                    object : RoomMembersService {
                        override suspend fun find(roomId: RoomId, userIds: List<UserId>) = roomService.findMembers(roomId, userIds)
                        override suspend fun findSummary(roomId: RoomId) = roomService.findMembersSummary(roomId)
                        override suspend fun insert(roomId: RoomId, members: List<RoomMember>) = roomService.insertMembers(roomId, members)
                    }
                },
                errorTracker = errorTracker,
                coroutineDispatchers = coroutineDispatchers,
            )

            installPushService(credentialsStore)
        }
    }

}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(mode = LazyThreadSafetyMode.NONE, initializer = initializer)
