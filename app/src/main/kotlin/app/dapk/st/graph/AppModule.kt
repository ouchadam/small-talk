package app.dapk.st.graph

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import app.dapk.db.DapkDb
import app.dapk.st.BuildConfig
import app.dapk.st.SharedPreferencesDelegate
import app.dapk.st.core.*
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.domain.StoreModule
import app.dapk.st.home.HomeModule
import app.dapk.st.home.MainActivity
import app.dapk.st.imageloader.ImageLoaderModule
import app.dapk.st.login.LoginModule
import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.MatrixTaskRunner
import app.dapk.st.matrix.MatrixTaskRunner.MatrixTask
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
import app.dapk.st.matrix.message.*
import app.dapk.st.matrix.push.installPushService
import app.dapk.st.matrix.push.pushService
import app.dapk.st.matrix.room.*
import app.dapk.st.matrix.sync.*
import app.dapk.st.matrix.sync.internal.request.ApiToDeviceEvent
import app.dapk.st.matrix.sync.internal.room.MessageDecrypter
import app.dapk.st.messenger.MessengerActivity
import app.dapk.st.messenger.MessengerModule
import app.dapk.st.navigator.IntentFactory
import app.dapk.st.notifications.NotificationsModule
import app.dapk.st.olm.DeviceKeyFactory
import app.dapk.st.olm.OlmPersistenceWrapper
import app.dapk.st.olm.OlmWrapper
import app.dapk.st.profile.ProfileModule
import app.dapk.st.push.PushModule
import app.dapk.st.settings.SettingsModule
import app.dapk.st.tracking.TrackingModule
import app.dapk.st.work.TaskRunner
import app.dapk.st.work.TaskRunnerModule
import app.dapk.st.work.WorkModule
import app.dapk.st.work.WorkScheduler
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import java.time.Clock

internal class AppModule(context: Application, logger: MatrixLogger) {

    private val buildMeta = BuildMeta(BuildConfig.VERSION_NAME)
    private val trackingModule by unsafeLazy {
        TrackingModule(
            isCrashTrackingEnabled = !BuildConfig.DEBUG
        )
    }

    private val driver = AndroidSqliteDriver(DapkDb.Schema, context, "dapk.db")
    private val database = DapkDb(driver)
    private val clock = Clock.systemUTC()
    private val coroutineDispatchers = CoroutineDispatchers(Dispatchers.IO)

    val storeModule = unsafeLazy {
        StoreModule(
            database = database,
            preferences = SharedPreferencesDelegate(context.applicationContext, fileName = "dapk-user-preferences", coroutineDispatchers),
            errorTracker = trackingModule.errorTracker,
            credentialPreferences = SharedPreferencesDelegate(context.applicationContext, fileName = "dapk-credentials-preferences", coroutineDispatchers),
            databaseDropper = { includeCryptoAccount ->
                val cursor = driver.executeQuery(
                    identifier = null,
                    sql = "SELECT name FROM sqlite_master WHERE type = 'table' ${if (includeCryptoAccount) "" else "AND name != 'dbCryptoAccount'"}",
                    parameters = 0
                )
                while (cursor.next()) {
                    cursor.getString(0)?.let {
                        driver.execute(null, "DELETE FROM $it", 0)
                    }
                }
            },
            coroutineDispatchers = coroutineDispatchers
        )
    }
    private val workModule = WorkModule(context)
    private val imageLoaderModule = ImageLoaderModule(context)

    private val matrixModules = MatrixModules(storeModule, trackingModule, workModule, logger, coroutineDispatchers)
    val domainModules = DomainModules(matrixModules, trackingModule.errorTracker)

    val coreAndroidModule = CoreAndroidModule(intentFactory = object : IntentFactory {
        override fun home(activity: Activity) = Intent(activity, MainActivity::class.java)
        override fun messenger(activity: Activity, roomId: RoomId) = MessengerActivity.newInstance(activity, roomId)
        override fun messengerShortcut(activity: Activity, roomId: RoomId) = MessengerActivity.newShortcutInstance(activity, roomId)
    })

    val featureModules = FeatureModules(
        storeModule,
        matrixModules,
        domainModules,
        trackingModule,
        imageLoaderModule,
        context,
        buildMeta,
        coroutineDispatchers,
        clock,
    )
}

internal class FeatureModules internal constructor(
    private val storeModule: Lazy<StoreModule>,
    private val matrixModules: MatrixModules,
    private val domainModules: DomainModules,
    private val trackingModule: TrackingModule,
    imageLoaderModule: ImageLoaderModule,
    context: Context,
    buildMeta: BuildMeta,
    coroutineDispatchers: CoroutineDispatchers,
    clock: Clock,
) {

    val directoryModule by unsafeLazy {
        DirectoryModule(
            syncService = matrixModules.sync,
            messageService = matrixModules.message,
            context = context,
            credentialsStore = storeModule.value.credentialsStore(),
            roomStore = storeModule.value.roomStore(),
            roomService = matrixModules.room,
        )
    }
    val loginModule by unsafeLazy {
        LoginModule(
            matrixModules.auth,
            domainModules.pushModule,
            matrixModules.profile,
            trackingModule.errorTracker
        )
    }
    val messengerModule by unsafeLazy {
        MessengerModule(
            matrixModules.sync,
            matrixModules.message,
            matrixModules.room,
            storeModule.value.credentialsStore(),
            storeModule.value.roomStore(),
            clock
        )
    }
    val homeModule by unsafeLazy { HomeModule(storeModule.value, matrixModules.profile) }
    val settingsModule by unsafeLazy {
        SettingsModule(
            storeModule.value,
            matrixModules.crypto,
            matrixModules.sync,
            context.contentResolver,
            buildMeta,
            coroutineDispatchers
        )
    }
    val profileModule by unsafeLazy { ProfileModule(matrixModules.profile, matrixModules.sync, matrixModules.room) }
    val notificationsModule by unsafeLazy {
        NotificationsModule(
            matrixModules.push,
            matrixModules.sync,
            storeModule.value.credentialsStore(),
            domainModules.pushModule.registerFirebasePushTokenUseCase(),
            imageLoaderModule.iconLoader(),
            storeModule.value.roomStore(),
            context,
        )
    }

}

internal class MatrixModules(
    private val storeModule: Lazy<StoreModule>,
    private val trackingModule: TrackingModule,
    private val workModule: WorkModule,
    private val logger: MatrixLogger,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    val matrix by unsafeLazy {
        val store = storeModule.value
        val credentialsStore = store.credentialsStore()
        MatrixClient(
            KtorMatrixHttpClientFactory(
                credentialsStore,
                includeLogging = true
            ),
            logger
        ).also {
            it.install {
                installAuthService(credentialsStore)
                installEncryptionService(store.knownDevicesStore())

                val olmAccountStore = OlmPersistenceWrapper(store.olmStore(), AndroidBase64())
                val singletonFlows = SingletonFlows(coroutineDispatchers)
                val olm = OlmWrapper(
                    olmStore = olmAccountStore,
                    singletonFlows = singletonFlows,
                    jsonCanonicalizer = JsonCanonicalizer(),
                    deviceKeyFactory = DeviceKeyFactory(JsonCanonicalizer()),
                    errorTracker = trackingModule.errorTracker,
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
                    coroutineDispatchers = coroutineDispatchers,
                )
                installMessageService(store.localEchoStore, BackgroundWorkAdapter(workModule.workScheduler())) { serviceProvider ->
                    MessageEncrypter { message ->
                        val result = serviceProvider.cryptoService().encrypt(
                            roomId = when (message) {
                                is MessageService.Message.TextMessage -> message.roomId
                            },
                            credentials = credentialsStore.credentials()!!,
                            when (message) {
                                is MessageService.Message.TextMessage -> JsonString(
                                    MatrixHttpClient.jsonWithDefaults.encodeToString(
                                        ApiMessage.TextMessage.serializer(),
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
                    storeModule.value.memberStore(),
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

                installProfileService(storeModule.value.profileStore(), singletonFlows, credentialsStore)

                installSyncService(
                    credentialsStore,
                    store.overviewStore(),
                    store.roomStore(),
                    store.syncStore(),
                    store.filterStore(),
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
                        logger.matrixLog(MatrixLogTag.VERIFICATION, "got a verification request $it")
                        val cryptoService = services.cryptoService()
                        VerificationHandler { apiEvent ->
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
                    deviceNotifier = { services ->
                        val encryption = services.deviceService()
                        val crypto = services.cryptoService()
                        DeviceNotifier { userIds, syncToken ->
                            encryption.updateStaleDevices(userIds)
                            crypto.updateOlmSession(userIds, syncToken)
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
                    errorTracker = trackingModule.errorTracker,
                    coroutineDispatchers = coroutineDispatchers,
                )

                installPushService(credentialsStore)


            }
        }
    }

    val auth by unsafeLazy { matrix.authService() }
    val push by unsafeLazy { matrix.pushService() }
    val sync by unsafeLazy { matrix.syncService() }
    val message by unsafeLazy { matrix.messageService() }
    val room by unsafeLazy { matrix.roomService() }
    val profile by unsafeLazy { matrix.profileService() }
    val crypto by unsafeLazy { matrix.cryptoService() }
}

internal class DomainModules(
    private val matrixModules: MatrixModules,
    private val errorTracker: ErrorTracker,
) {

    val pushModule by unsafeLazy { PushModule(matrixModules.push, errorTracker) }
    val taskRunnerModule by unsafeLazy { TaskRunnerModule(TaskRunnerAdapter(matrixModules.matrix::run)) }
}

class BackgroundWorkAdapter(private val workScheduler: WorkScheduler) : BackgroundScheduler {
    override fun schedule(key: String, task: BackgroundScheduler.Task) {
        workScheduler.schedule(
            WorkScheduler.WorkTask(
                jobId = 1,
                type = task.type,
                jsonPayload = task.jsonPayload,
            )
        )
    }
}

class TaskRunnerAdapter(private val matrixTaskRunner: suspend (MatrixTask) -> MatrixTaskRunner.TaskResult) : TaskRunner {

    override suspend fun run(tasks: List<TaskRunner.RunnableWorkTask>): List<TaskRunner.TaskResult> {
        return tasks.map {
            when (val result = matrixTaskRunner(MatrixTask(it.task.type, it.task.jsonPayload))) {
                is MatrixTaskRunner.TaskResult.Failure -> TaskRunner.TaskResult.Failure(it.source, canRetry = result.canRetry)
                MatrixTaskRunner.TaskResult.Success -> TaskRunner.TaskResult.Success(it.source)
            }
        }
    }
}

class AndroidBase64 : Base64 {
    override fun encode(input: ByteArray): String {
        return android.util.Base64.encodeToString(input, android.util.Base64.DEFAULT)
    }

    override fun decode(input: String): ByteArray {
        return android.util.Base64.decode(input, android.util.Base64.DEFAULT)
    }
}