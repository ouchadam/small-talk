package app.dapk.st.graph

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.dapk.db.DapkDb
import app.dapk.db.app.StDb
import app.dapk.engine.core.Base64
import app.dapk.st.BuildConfig
import app.dapk.st.core.*
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.domain.MatrixStoreModule
import app.dapk.st.domain.StoreModule
import app.dapk.st.engine.ImageContentReader
import app.dapk.st.engine.MatrixEngine
import app.dapk.st.firebase.messaging.MessagingModule
import app.dapk.st.home.BetaVersionUpgradeUseCase
import app.dapk.st.home.HomeModule
import app.dapk.st.home.MainActivity
import app.dapk.st.imageloader.ImageLoaderModule
import app.dapk.st.impl.*
import app.dapk.st.login.LoginModule
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.JsonString
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.messenger.MessengerActivity
import app.dapk.st.messenger.MessengerModule
import app.dapk.st.messenger.gallery.ImageGalleryModule
import app.dapk.st.navigator.IntentFactory
import app.dapk.st.navigator.MessageAttachment
import app.dapk.st.notifications.NotificationsModule
import app.dapk.st.profile.ProfileModule
import app.dapk.st.push.PushHandler
import app.dapk.st.push.PushModule
import app.dapk.st.push.PushTokenPayload
import app.dapk.st.push.messaging.MessagingServiceAdapter
import app.dapk.st.settings.SettingsModule
import app.dapk.st.share.ShareEntryModule
import app.dapk.st.tracking.TrackingModule
import app.dapk.st.work.TaskRunnerModule
import app.dapk.st.work.WorkModule
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

internal class AppModule(context: Application, logger: MatrixLogger) {

    private val buildMeta = BuildMeta(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, isDebug = BuildConfig.DEBUG)
    private val deviceMeta = DeviceMeta(Build.VERSION.SDK_INT)
    private val trackingModule by unsafeLazy {
        TrackingModule(
            isCrashTrackingEnabled = !BuildConfig.DEBUG
        )
    }

    private val driver = AndroidSqliteDriver(DapkDb.Schema, context, "dapk.db")
    private val stDriver = AndroidSqliteDriver(StDb.Schema, context, "stdb.db")
    private val engineDatabase = DapkDb(driver)
    private val stDatabase = StDb(stDriver)
    val coroutineDispatchers = CoroutineDispatchers(Dispatchers.IO)
    private val base64 = AndroidBase64()

    val storeModule = unsafeLazy {
        StoreModule(
            database = stDatabase,
            preferences = SharedPreferencesDelegate(context.applicationContext, fileName = "dapk-user-preferences", coroutineDispatchers),
            credentialPreferences = SharedPreferencesDelegate(context.applicationContext, fileName = "dapk-credentials-preferences", coroutineDispatchers),
            databaseDropper = DefaultDatabaseDropper(coroutineDispatchers, driver),
            coroutineDispatchers = coroutineDispatchers
        )
    }


    private val workModule = WorkModule(context)
    private val imageLoaderModule = ImageLoaderModule(context)

    private val imageContentReader by unsafeLazy { AndroidImageContentReader(context.contentResolver) }
    private val chatEngineModule = ChatEngineModule(
        unsafeLazy { matrixStoreModule() },
        trackingModule,
        workModule,
        logger,
        coroutineDispatchers,
        imageContentReader,
        base64,
        buildMeta
    )

    private fun matrixStoreModule(): MatrixStoreModule {
        val value = storeModule.value
        return MatrixStoreModule(
            engineDatabase,
            value.preferences.engine(),
            value.credentialPreferences.engine(),
            trackingModule.errorTracker.engine(),
            coroutineDispatchers.engine(),
        )
    }

    val domainModules = DomainModules(chatEngineModule, trackingModule.errorTracker, context, coroutineDispatchers)

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
        unsafeLazy { storeModule.value.cachingPreferences },
    )

    val featureModules = FeatureModules(
        storeModule,
        chatEngineModule,
        domainModules,
        trackingModule,
        coreAndroidModule,
        imageLoaderModule,
        context,
        buildMeta,
        deviceMeta,
        coroutineDispatchers,
    )
}

internal class FeatureModules internal constructor(
    private val storeModule: Lazy<StoreModule>,
    val chatEngineModule: ChatEngineModule,
    private val domainModules: DomainModules,
    private val trackingModule: TrackingModule,
    private val coreAndroidModule: CoreAndroidModule,
    imageLoaderModule: ImageLoaderModule,
    context: Context,
    buildMeta: BuildMeta,
    deviceMeta: DeviceMeta,
    coroutineDispatchers: CoroutineDispatchers,
) {

    val directoryModule by unsafeLazy {
        DirectoryModule(
            context = context,
            chatEngine = chatEngineModule.engine,
            iconLoader = imageLoaderModule.iconLoader(),
        )
    }
    val loginModule by unsafeLazy {
        LoginModule(
            chatEngineModule.engine,
            domainModules.pushModule,
            trackingModule.errorTracker
        )
    }
    val messengerModule by unsafeLazy {
        MessengerModule(
            chatEngineModule.engine,
            context,
            storeModule.value.messageStore(),
            deviceMeta,
        )
    }
    val homeModule by unsafeLazy {
        HomeModule(
            chatEngineModule.engine,
            storeModule.value,
            BetaVersionUpgradeUseCase(
                storeModule.value.applicationStore(),
                buildMeta,
            ),
            profileModule,
            loginModule,
            directoryModule
        )
    }
    val settingsModule by unsafeLazy {
        SettingsModule(
            chatEngineModule.engine,
            storeModule.value,
            pushModule,
            context.contentResolver,
            buildMeta,
            deviceMeta,
            coroutineDispatchers,
            coreAndroidModule.themeStore(),
            storeModule.value.loggingStore(),
            storeModule.value.messageStore(),
        )
    }
    val profileModule by unsafeLazy { ProfileModule(chatEngineModule.engine, trackingModule.errorTracker) }
    val notificationsModule by unsafeLazy {
        NotificationsModule(
            chatEngineModule.engine,
            imageLoaderModule.iconLoader(),
            context,
            intentFactory = coreAndroidModule.intentFactory(),
            dispatchers = coroutineDispatchers,
            deviceMeta = deviceMeta
        )
    }

    val shareEntryModule by unsafeLazy {
        ShareEntryModule(chatEngineModule.engine)
    }

    val imageGalleryModule by unsafeLazy {
        ImageGalleryModule(context.contentResolver, coroutineDispatchers)
    }

    val pushModule by unsafeLazy {
        domainModules.pushModule
    }

    val messagingModule by unsafeLazy {
        domainModules.messaging
    }

}

internal class ChatEngineModule(
    private val matrixStoreModule: Lazy<MatrixStoreModule>,
    private val trackingModule: TrackingModule,
    private val workModule: WorkModule,
    private val logger: MatrixLogger,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val imageContentReader: ImageContentReader,
    private val base64: Base64,
    private val buildMeta: BuildMeta,
) {

    val engine by unsafeLazy {
        val matrixCoroutineDispatchers = app.dapk.engine.core.CoroutineDispatchers(
            coroutineDispatchers.io,
            coroutineDispatchers.main,
            coroutineDispatchers.global
        )
        val matrixStore = matrixStoreModule.value
        MatrixEngine.Factory().create(
            base64,
            logger,
            SmallTalkDeviceNameGenerator(),
            matrixCoroutineDispatchers,
            trackingModule.errorTracker.engine(),
            imageContentReader,
            BackgroundWorkAdapter(workModule.workScheduler()),
            matrixStore,
            includeLogging = buildMeta.isDebug,
        )
    }

}

internal class DomainModules(
    private val chatEngineModule: ChatEngineModule,
    private val errorTracker: ErrorTracker,
    private val context: Application,
    private val dispatchers: CoroutineDispatchers,
) {

    private val pushHandler by unsafeLazy {
        val enginePushHandler = chatEngineModule.engine.pushHandler()
        object : PushHandler {
            override fun onNewToken(payload: PushTokenPayload) {
                enginePushHandler.onNewToken(JsonString(Json.encodeToString(PushTokenPayload.serializer(), payload)))
            }

            override fun onMessageReceived(eventId: EventId?, roomId: RoomId?) = enginePushHandler.onMessageReceived(eventId, roomId)
        }
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
    val taskRunnerModule by unsafeLazy {
        TaskRunnerModule(TaskRunnerAdapter(chatEngineModule.engine, AppTaskRunner(chatEngineModule.engine)))
    }
}

private fun CoroutineDispatchers.engine() = app.dapk.engine.core.CoroutineDispatchers(this.io, this.main, this.global)

private fun ErrorTracker.engine(): app.dapk.engine.core.extensions.ErrorTracker {
    val tracker = this
    return object : app.dapk.engine.core.extensions.ErrorTracker {
        override fun track(throwable: Throwable, extra: String) = tracker.track(throwable, extra)
    }
}

private fun Preferences.engine(): app.dapk.engine.core.Preferences {
    val prefs = this
    return object : app.dapk.engine.core.Preferences {
        override suspend fun store(key: String, value: String) = prefs.store(key, value)
        override suspend fun readString(key: String) = prefs.readString(key)
        override suspend fun clear() = prefs.clear()
        override suspend fun remove(key: String) = prefs.remove(key)
    }
}