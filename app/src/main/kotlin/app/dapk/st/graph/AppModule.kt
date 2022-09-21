package app.dapk.st.graph

import android.app.Application
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import app.dapk.db.DapkDb
import app.dapk.st.BuildConfig
import app.dapk.st.SharedPreferencesDelegate
import app.dapk.st.core.*
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.domain.StoreModule
import app.dapk.st.firebase.messaging.MessagingModule
import app.dapk.st.home.HomeModule
import app.dapk.st.home.MainActivity
import app.dapk.st.imageloader.ImageLoaderModule
import app.dapk.st.login.LoginModule
import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.auth.authService
import app.dapk.st.matrix.auth.installAuthService
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.RoomMembersProvider
import app.dapk.st.matrix.crypto.Verification
import app.dapk.st.matrix.crypto.cryptoService
import app.dapk.st.matrix.crypto.installCryptoService
import app.dapk.st.matrix.device.deviceService
import app.dapk.st.matrix.device.installEncryptionService
import app.dapk.st.matrix.http.ktor.KtorMatrixHttpClientFactory
import app.dapk.st.matrix.message.MessageEncrypter
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.message.installMessageService
import app.dapk.st.matrix.message.internal.ImageContentReader
import app.dapk.st.matrix.message.messageService
import app.dapk.st.matrix.push.installPushService
import app.dapk.st.matrix.push.pushService
import app.dapk.st.matrix.room.*
import app.dapk.st.matrix.sync.*
import app.dapk.st.matrix.sync.internal.request.ApiToDeviceEvent
import app.dapk.st.matrix.sync.internal.room.MessageDecrypter
import app.dapk.st.messenger.MessengerActivity
import app.dapk.st.messenger.MessengerModule
import app.dapk.st.navigator.IntentFactory
import app.dapk.st.navigator.MessageAttachment
import app.dapk.st.notifications.MatrixPushHandler
import app.dapk.st.notifications.NotificationsModule
import app.dapk.st.olm.DeviceKeyFactory
import app.dapk.st.olm.OlmPersistenceWrapper
import app.dapk.st.olm.OlmWrapper
import app.dapk.st.profile.ProfileModule
import app.dapk.st.push.PushModule
import app.dapk.st.push.messaging.MessagingServiceAdapter
import app.dapk.st.settings.SettingsModule
import app.dapk.st.share.ShareEntryModule
import app.dapk.st.tracking.TrackingModule
import app.dapk.st.work.TaskRunnerModule
import app.dapk.st.work.WorkModule
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import java.time.Clock

internal class AppModule(context: Application, logger: MatrixLogger) {

    private val buildMeta = BuildMeta(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, isDebug = BuildConfig.DEBUG)
    private val deviceMeta = DeviceMeta(Build.VERSION.SDK_INT)
    private val trackingModule by unsafeLazy {
        TrackingModule(
            isCrashTrackingEnabled = !BuildConfig.DEBUG
        )
    }

    private val driver = AndroidSqliteDriver(DapkDb.Schema, context, "dapk.db")
    private val database = DapkDb(driver)
    private val clock = Clock.systemUTC()
    val coroutineDispatchers = CoroutineDispatchers(Dispatchers.IO)

    val storeModule = unsafeLazy {
        StoreModule(
            database = database,
            preferences = SharedPreferencesDelegate(context.applicationContext, fileName = "dapk-user-preferences", coroutineDispatchers),
            errorTracker = trackingModule.errorTracker,
            credentialPreferences = SharedPreferencesDelegate(context.applicationContext, fileName = "dapk-credentials-preferences", coroutineDispatchers),
            databaseDropper = DefaultDatabaseDropper(coroutineDispatchers, driver),
            coroutineDispatchers = coroutineDispatchers
        )
    }
    private val workModule = WorkModule(context)
    private val imageLoaderModule = ImageLoaderModule(context)

    private val matrixModules = MatrixModules(storeModule, trackingModule, workModule, logger, coroutineDispatchers, context.contentResolver, buildMeta)
    val domainModules = DomainModules(matrixModules, trackingModule.errorTracker, workModule, storeModule, context, coroutineDispatchers)

    val coreAndroidModule = CoreAndroidModule(
        intentFactory = object : IntentFactory {
            override fun notificationOpenApp(context: Context) = PendingIntent.getActivity(
                context,
                1000,
                home(context)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            override fun notificationOpenMessage(context: Context, roomId: RoomId) = PendingIntent.getActivity(
                context,
                roomId.hashCode(),
                messenger(context, roomId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            override fun home(context: Context) = Intent(context, MainActivity::class.java)
            override fun messenger(context: Context, roomId: RoomId) = MessengerActivity.newInstance(context, roomId)
            override fun messengerShortcut(context: Context, roomId: RoomId) = MessengerActivity.newShortcutInstance(context, roomId)
            override fun messengerAttachments(context: Context, roomId: RoomId, attachments: List<MessageAttachment>) = MessengerActivity.newMessageAttachment(
                context,
                roomId,
                attachments
            )
        },
        unsafeLazy { storeModule.value.preferences }
    )

    val featureModules = FeatureModules(
        storeModule,
        matrixModules,
        domainModules,
        trackingModule,
        coreAndroidModule,
        imageLoaderModule,
        context,
        buildMeta,
        deviceMeta,
        coroutineDispatchers,
        clock,
    )
}

internal class FeatureModules internal constructor(
    private val storeModule: Lazy<StoreModule>,
    private val matrixModules: MatrixModules,
    private val domainModules: DomainModules,
    private val trackingModule: TrackingModule,
    private val coreAndroidModule: CoreAndroidModule,
    imageLoaderModule: ImageLoaderModule,
    context: Context,
    buildMeta: BuildMeta,
    deviceMeta: DeviceMeta,
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
    val homeModule by unsafeLazy { HomeModule(storeModule.value, matrixModules.profile, matrixModules.sync, buildMeta) }
    val settingsModule by unsafeLazy {
        SettingsModule(
            storeModule.value,
            pushModule,
            matrixModules.crypto,
            matrixModules.sync,
            context.contentResolver,
            buildMeta,
            deviceMeta,
            coroutineDispatchers,
            coreAndroidModule.themeStore(),
        )
    }
    val profileModule by unsafeLazy { ProfileModule(matrixModules.profile, matrixModules.sync, matrixModules.room, trackingModule.errorTracker) }
    val notificationsModule by unsafeLazy {
        NotificationsModule(
            imageLoaderModule.iconLoader(),
            storeModule.value.roomStore(),
            storeModule.value.overviewStore(),
            context,
            intentFactory = coreAndroidModule.intentFactory(),
            dispatchers = coroutineDispatchers,
            deviceMeta = deviceMeta
        )
    }

    val shareEntryModule by unsafeLazy {
        ShareEntryModule(matrixModules.sync, matrixModules.room)
    }

    val pushModule by unsafeLazy {
        domainModules.pushModule
    }

    val messagingModule by unsafeLazy {
        domainModules.messaging
    }

}

internal class MatrixModules(
    private val storeModule: Lazy<StoreModule>,
    private val trackingModule: TrackingModule,
    private val workModule: WorkModule,
    private val logger: MatrixLogger,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val contentResolver: ContentResolver,
    private val buildMeta: BuildMeta,
) {

    val matrix by unsafeLazy {
        val store = storeModule.value
        val credentialsStore = store.credentialsStore()
        MatrixClient(
            KtorMatrixHttpClientFactory(
                credentialsStore,
                includeLogging = buildMeta.isDebug,
            ),
            logger
        ).also {
            it.install {
                installAuthService(credentialsStore)
                installEncryptionService(store.knownDevicesStore())

                val base64 = AndroidBase64()
                val olmAccountStore = OlmPersistenceWrapper(store.olmStore(), base64)
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
                    base64 = base64,
                    coroutineDispatchers = coroutineDispatchers,
                )
                val imageContentReader = AndroidImageContentReader(contentResolver)
                installMessageService(store.localEchoStore, BackgroundWorkAdapter(workModule.workScheduler()), imageContentReader, base64) { serviceProvider ->
                    MessageEncrypter { message ->
                        val result = serviceProvider.cryptoService().encrypt(
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
                }

                val overviewStore = store.overviewStore()
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
                    },
                    roomInviteRemover = {
                        overviewStore.removeInvites(listOf(it))
                    }
                )

                installProfileService(storeModule.value.profileStore(), singletonFlows, credentialsStore)

                installSyncService(
                    credentialsStore,
                    overviewStore,
                    store.roomStore(),
                    store.syncStore(),
                    store.filterStore(),
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
    private val workModule: WorkModule,
    private val storeModule: Lazy<StoreModule>,
    private val context: Application,
    private val dispatchers: CoroutineDispatchers,
) {

    val pushHandler by unsafeLazy {
        val store = storeModule.value
        MatrixPushHandler(
            workScheduler = workModule.workScheduler(),
            credentialsStore = store.credentialsStore(),
            matrixModules.sync,
            store.roomStore(),
        )
    }

    val messaging by unsafeLazy { MessagingModule(MessagingServiceAdapter(pushHandler), context) }

    val pushModule by unsafeLazy {
        PushModule(
            errorTracker,
            pushHandler,
            context,
            dispatchers,
            SharedPreferencesDelegate(context.applicationContext, fileName = "dapk-user-preferences", dispatchers),
            messaging.messaging,
        )
    }
    val taskRunnerModule by unsafeLazy { TaskRunnerModule(TaskRunnerAdapter(matrixModules.matrix::run, AppTaskRunner(matrixModules.push))) }

}

internal class AndroidImageContentReader(private val contentResolver: ContentResolver) : ImageContentReader {
    override fun read(uri: String): ImageContentReader.ImageContent {
        val androidUri = Uri.parse(uri)
        val fileStream = contentResolver.openInputStream(androidUri) ?: throw IllegalArgumentException("Could not process $uri")

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(fileStream, null, options)

        return contentResolver.openInputStream(androidUri)?.use { stream ->
            val output = stream.readBytes()
            ImageContentReader.ImageContent(
                height = options.outHeight,
                width = options.outWidth,
                size = output.size.toLong(),
                mimeType = options.outMimeType,
                fileName = androidUri.lastPathSegment ?: "file",
                content = output
            )
        } ?: throw IllegalArgumentException("Could not process $uri")
    }
}