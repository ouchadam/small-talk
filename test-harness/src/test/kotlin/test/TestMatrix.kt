package test

import TestUser
import app.dapk.st.core.Base64
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.SingletonFlows
import app.dapk.st.domain.StoreModule
import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.auth.AuthConfig
import app.dapk.st.matrix.auth.authService
import app.dapk.st.matrix.auth.installAuthService
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.RoomMembersProvider
import app.dapk.st.matrix.crypto.Verification
import app.dapk.st.matrix.crypto.cryptoService
import app.dapk.st.matrix.crypto.installCryptoService
import app.dapk.st.matrix.device.deviceService
import app.dapk.st.matrix.device.installEncryptionService
import app.dapk.st.matrix.device.internal.ApiMessage
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.ktor.KtorMatrixHttpClientFactory
import app.dapk.st.matrix.message.MessageEncrypter
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.message.installMessageService
import app.dapk.st.matrix.message.messageService
import app.dapk.st.matrix.push.installPushService
import app.dapk.st.matrix.room.RoomMessenger
import app.dapk.st.matrix.room.installRoomService
import app.dapk.st.matrix.room.roomService
import app.dapk.st.matrix.sync.*
import app.dapk.st.matrix.sync.internal.request.ApiToDeviceEvent
import app.dapk.st.matrix.sync.internal.room.MessageDecrypter
import app.dapk.st.olm.DeviceKeyFactory
import app.dapk.st.olm.OlmPersistenceWrapper
import app.dapk.st.olm.OlmWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import test.impl.InMemoryDatabase
import test.impl.InMemoryPreferences
import test.impl.InstantScheduler
import test.impl.PrintingErrorTracking
import java.time.Clock

class TestMatrix(
    private val user: TestUser,
    includeHttpLogging: Boolean = false,
    includeLogging: Boolean = false,
) {

    private val errorTracker = PrintingErrorTracking(prefix = user.roomMember.id.value.split(":")[0])
    private val logger: MatrixLogger = { tag, message ->
        if (includeLogging) {
            println("${user.roomMember.id.value.split(":")[0]}  $tag $message")
        }
    }

    private val preferences = InMemoryPreferences()
    private val database = InMemoryDatabase.realInstance(user.roomMember.id.value)
    private val coroutineDispatchers = CoroutineDispatchers(Dispatchers.Unconfined, CoroutineScope(Dispatchers.Unconfined))

    private val storeModule = StoreModule(
        database = database,
        preferences = preferences,
        errorTracker = errorTracker,
        credentialPreferences = preferences,
        databaseDropper = {
            // do nothing
        },
        coroutineDispatchers = coroutineDispatchers
    )

    val client = MatrixClient(
        KtorMatrixHttpClientFactory(
            storeModule.credentialsStore(),
            includeLogging = includeHttpLogging,
        ),
        logger
    ).also {
        it.install {
            installAuthService(storeModule.credentialsStore(), AuthConfig(forceHttp = false))
            installEncryptionService(storeModule.knownDevicesStore())

            val olmAccountStore = OlmPersistenceWrapper(storeModule.olmStore(), JavaBase64())
            val olm = OlmWrapper(
                olmStore = olmAccountStore,
                singletonFlows = SingletonFlows(coroutineDispatchers),
                jsonCanonicalizer = JsonCanonicalizer(),
                deviceKeyFactory = DeviceKeyFactory(JsonCanonicalizer()),
                errorTracker = errorTracker,
                logger = logger,
                clock = Clock.systemUTC(),
                coroutineDispatchers = coroutineDispatchers,
            )
            installCryptoService(
                storeModule.credentialsStore(),
                olm,
                roomMembersProvider = { services ->
                    RoomMembersProvider {
                        services.roomService().joinedMembers(it).map { it.userId }
                    }
                },
                coroutineDispatchers = coroutineDispatchers,
            )

            installMessageService(storeModule.localEchoStore, InstantScheduler(it)) { serviceProvider ->
                MessageEncrypter { message ->
                    val result = serviceProvider.cryptoService().encrypt(
                        roomId = when (message) {
                            is MessageService.Message.TextMessage -> message.roomId
                        },
                        credentials = storeModule.credentialsStore().credentials()!!,
                        when (message) {
                            is MessageService.Message.TextMessage -> JsonString(
                                MatrixHttpClient.jsonWithDefaults.encodeToString(
                                    ApiMessage.TextMessage(
                                        ApiMessage.TextMessage.TextContent(
                                            message.content.body,
                                            message.content.type,
                                        ), message.roomId, type = EventType.ROOM_MESSAGE.value
                                    )
                                )
                            )
                        }
                    )

                    MessageEncrypter.EncryptedMessagePayload(
                        result.algorithmName,
                        result.senderKey,
                        result.cipherText,
                        result.sessionId,
                        result.deviceId,
                    )
                }
            }

            installRoomService(
                storeModule.memberStore(),
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
                }
            )

            installSyncService(
                storeModule.credentialsStore(),
                storeModule.overviewStore(),
                storeModule.roomStore(),
                storeModule.syncStore(),
                storeModule.filterStore(),
                messageDecrypter = { serviceProvider ->
                    MessageDecrypter {
                        serviceProvider.cryptoService().decrypt(it)
                    }
                },
                keySharer = { serviceProvider ->
                    KeySharer { sharedRoomKeys ->
                        serviceProvider.cryptoService().importRoomKeys(sharedRoomKeys)
                    }
                },
                verificationHandler = { services ->
                    val cryptoService = services.cryptoService()
                    VerificationHandler { apiEvent ->
                        logger.matrixLog(MatrixLogTag.VERIFICATION, "got a verification request $apiEvent")
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
                                is ApiToDeviceEvent.VerificationAccept -> Verification.Event.Accepted(
                                    apiEvent.sender,
                                    apiEvent.content.fromDevice,
                                    apiEvent.content.method,
                                    apiEvent.content.protocol,
                                    apiEvent.content.hash,
                                    apiEvent.content.code,
                                    apiEvent.content.short,
                                    apiEvent.content.transactionId,
                                )
                                is ApiToDeviceEvent.VerificationCancel -> TODO()
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
                deviceNotifier = { services ->
                    val encryptionService = services.deviceService()
                    val cryptoService = services.cryptoService()
                    DeviceNotifier { userIds, syncToken ->
                        encryptionService.updateStaleDevices(userIds)
                        cryptoService.updateOlmSession(userIds, syncToken)
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
                        override suspend fun insert(roomId: RoomId, members: List<RoomMember>) = roomService.insertMembers(roomId, members)
                    }
                },
                errorTracker = errorTracker,
                coroutineDispatchers = coroutineDispatchers,
                syncConfig = SyncConfig(loopTimeout = 500, allowSharedFlows = false)
            )
            installPushService(storeModule.credentialsStore())
        }
    }

    suspend fun newlogin() {
        client.authService()
            .login(user.roomMember.id.value, user.password)
    }

    suspend fun restoreLogin() {
        val userId = this@TestMatrix.user.roomMember.id
        val json = TestPersistence(prefix = "").readJson("credentials-${userId.value}.json")!!
        val credentials = Json.decodeFromString(UserCredentials.serializer(), json)
        storeModule.credentialsStore().update(credentials)
        logger.matrixLog("restored: ${credentials.userId} : ${credentials.deviceId}")
    }

    suspend fun saveLogin(result: UserCredentials) {
        val userId = result.userId
        TestPersistence(prefix = "").put("credentials-${userId.value}.json", UserCredentials.serializer(), result)
    }

    suspend fun deviceId() = storeModule.credentialsStore().credentials()!!.deviceId
    suspend fun userId() = storeModule.credentialsStore().credentials()!!.userId
}

class JavaBase64 : Base64 {
    override fun encode(input: ByteArray): String {
        return java.util.Base64.getEncoder().encode(input).toString(Charsets.UTF_8)
    }

    override fun decode(input: String): ByteArray {
        return java.util.Base64.getDecoder().decode(input)
    }
}