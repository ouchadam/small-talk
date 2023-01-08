package app.dapk.st.settings

import android.content.ContentResolver
import app.dapk.st.core.*
import app.dapk.st.domain.StoreModule
import app.dapk.st.domain.application.eventlog.LoggingStore
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.engine.ChatEngine
import app.dapk.st.push.PushModule
import app.dapk.st.settings.eventlogger.EventLoggerViewModel
import app.dapk.st.settings.state.SettingsState
import app.dapk.st.settings.state.settingsReducer
import app.dapk.st.state.createStateViewModel

class SettingsModule(
    private val chatEngine: ChatEngine,
    private val storeModule: StoreModule,
    private val pushModule: PushModule,
    private val contentResolver: ContentResolver,
    private val buildMeta: BuildMeta,
    private val deviceMeta: DeviceMeta,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val themeStore: ThemeStore,
    private val loggingStore: LoggingStore,
    private val messageOptionsStore: MessageOptionsStore,
) : ProvidableModule {

    internal fun settingsState(): SettingsState {
        return createStateViewModel {
            settingsReducer(
                chatEngine,
                storeModule.cacheCleaner(),
                contentResolver,
                UriFilenameResolver(contentResolver, coroutineDispatchers),
                SettingsItemFactory(buildMeta, deviceMeta, pushModule.pushTokenRegistrars(), themeStore, loggingStore, messageOptionsStore),
                pushModule.pushTokenRegistrars(),
                themeStore,
                loggingStore,
                messageOptionsStore,
                it,
                JobBag(),
            )
        }
    }


    internal fun eventLogViewModel(): EventLoggerViewModel {
        return EventLoggerViewModel(storeModule.eventLogStore())
    }
}