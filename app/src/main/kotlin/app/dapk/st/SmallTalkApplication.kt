package app.dapk.st

import android.app.Application
import android.util.Log
import app.dapk.st.core.CoreAndroidModule
import app.dapk.st.core.ModuleProvider
import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.attachAppLogger
import app.dapk.st.core.extensions.Scope
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.messenger.MessengerModule
import app.dapk.st.graph.AppModule
import app.dapk.st.graph.FeatureModules
import app.dapk.st.home.HomeModule
import app.dapk.st.login.LoginModule
import app.dapk.st.notifications.NotificationsModule
import app.dapk.st.profile.ProfileModule
import app.dapk.st.settings.SettingsModule
import app.dapk.st.work.TaskRunnerModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class SmallTalkApplication : Application(), ModuleProvider {

    private val appLogger: (String, String) -> Unit = { tag, message -> _appLogger?.invoke(tag, message) }
    private var _appLogger: ((String, String) -> Unit)? = null

    private val appModule: AppModule by unsafeLazy { AppModule(this, appLogger) }
    private val featureModules: FeatureModules by unsafeLazy { appModule.featureModules }
    private val applicationScope = Scope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val notificationsModule = featureModules.notificationsModule
        val storeModule = appModule.storeModule.value
        val eventLogStore = storeModule.eventLogStore()

        val logger: (String, String) -> Unit = { tag, message ->
            Log.e(tag, message)
            GlobalScope.launch {
                eventLogStore.insert(tag, message)
            }
        }
        attachAppLogger(logger)
        _appLogger = logger

        applicationScope.launch {
            notificationsModule.firebasePushTokenUseCase().registerCurrentToken()
            storeModule.localEchoStore.preload()
        }

        applicationScope.launch {
            val notificationsUseCase = notificationsModule.notificationsUseCase()
            notificationsUseCase.listenForNotificationChanges()
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
            MessengerModule::class -> featureModules.messengerModule
            TaskRunnerModule::class -> appModule.domainModules.taskRunnerModule
            CoreAndroidModule::class -> appModule.coreAndroidModule
            else -> throw IllegalArgumentException("Unknown: $klass")
        } as T
    }
}
