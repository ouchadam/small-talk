package app.dapk.st

import android.app.Application
import android.util.Log
import app.dapk.st.core.CoreAndroidModule
import app.dapk.st.core.ModuleProvider
import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.attachAppLogger
import app.dapk.st.core.extensions.ResettableUnsafeLazy
import app.dapk.st.core.extensions.Scope
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.domain.StoreModule
import app.dapk.st.firebase.messaging.MessagingModule
import app.dapk.st.graph.AppModule
import app.dapk.st.home.HomeModule
import app.dapk.st.login.LoginModule
import app.dapk.st.messenger.MessengerModule
import app.dapk.st.notifications.NotificationsModule
import app.dapk.st.profile.ProfileModule
import app.dapk.st.push.PushModule
import app.dapk.st.settings.SettingsModule
import app.dapk.st.share.ShareEntryModule
import app.dapk.st.work.TaskRunnerModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.reflect.KClass

class SmallTalkApplication : Application(), ModuleProvider {

    private val appLogger: (String, String) -> Unit = { tag, message -> _appLogger?.invoke(tag, message) }
    private var _appLogger: ((String, String) -> Unit)? = null

    private val lazyAppModule = ResettableUnsafeLazy { AppModule(this, appLogger) }
    private val lazyFeatureModules = ResettableUnsafeLazy { appModule.featureModules }
    private val appModule by lazyAppModule
    private val featureModules by lazyFeatureModules
    private val applicationScope = Scope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val notificationsModule = featureModules.notificationsModule
        val storeModule = appModule.storeModule.value
        val eventLogStore = storeModule.eventLogStore()

        val logger: (String, String) -> Unit = { tag, message ->
            Log.e(tag, message)
            applicationScope.launch { eventLogStore.insert(tag, message) }
        }
        attachAppLogger(logger)
        _appLogger = logger

        onApplicationLaunch(notificationsModule, storeModule)
    }

    private fun onApplicationLaunch(notificationsModule: NotificationsModule, storeModule: StoreModule) {
        applicationScope.launch {
            featureModules.pushModule.pushTokenRegistrar().registerCurrentToken()
            storeModule.localEchoStore.preload()
        }

        applicationScope.launch {
            val notificationsUseCase = notificationsModule.notificationsUseCase()
            notificationsUseCase.listenForNotificationChanges(this)
        }
    }

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    override fun <T : ProvidableModule> provide(klass: KClass<T>): T {
        return when (klass) {
            DirectoryModule::class -> featureModules.directoryModule
            LoginModule::class -> featureModules.loginModule
            HomeModule::class -> featureModules.homeModule
            SettingsModule::class -> featureModules.settingsModule
            ProfileModule::class -> featureModules.profileModule
            NotificationsModule::class -> featureModules.notificationsModule
            PushModule::class -> featureModules.pushModule
            MessagingModule::class -> featureModules.messagingModule
            MessengerModule::class -> featureModules.messengerModule
            TaskRunnerModule::class -> appModule.domainModules.taskRunnerModule
            CoreAndroidModule::class -> appModule.coreAndroidModule
            ShareEntryModule::class -> featureModules.shareEntryModule
            else -> throw IllegalArgumentException("Unknown: $klass")
        } as T
    }

    override fun reset() {
        featureModules.pushModule.pushTokenRegistrar().unregister()
        appModule.coroutineDispatchers.io.cancel()
        applicationScope.cancel()
        lazyAppModule.reset()
        lazyFeatureModules.reset()

        val notificationsModule = featureModules.notificationsModule
        val storeModule = appModule.storeModule.value
        onApplicationLaunch(notificationsModule, storeModule)
    }
}
