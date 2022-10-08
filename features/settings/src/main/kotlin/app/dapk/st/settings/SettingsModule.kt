package app.dapk.st.settings

import android.content.ContentResolver
import app.dapk.st.core.*
import app.dapk.st.domain.StoreModule
import app.dapk.st.domain.application.eventlog.LoggingStore
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.matrix.crypto.CryptoService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.push.PushModule
import app.dapk.st.settings.eventlogger.EventLoggerViewModel

class SettingsModule(
    private val storeModule: StoreModule,
    private val pushModule: PushModule,
    private val cryptoService: CryptoService,
    private val syncService: SyncService,
    private val contentResolver: ContentResolver,
    private val buildMeta: BuildMeta,
    private val deviceMeta: DeviceMeta,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val themeStore: ThemeStore,
    private val loggingStore: LoggingStore,
    private val messageOptionsStore: MessageOptionsStore,
) : ProvidableModule {

    internal fun settingsViewModel(): SettingsViewModel {
        return SettingsViewModel(
            storeModule.cacheCleaner(),
            contentResolver,
            cryptoService,
            syncService,
            UriFilenameResolver(contentResolver, coroutineDispatchers),
            SettingsItemFactory(buildMeta, deviceMeta, pushModule.pushTokenRegistrars(), themeStore, loggingStore, messageOptionsStore),
            pushModule.pushTokenRegistrars(),
            themeStore,
            loggingStore,
            messageOptionsStore,
        )
    }

    internal fun eventLogViewModel(): EventLoggerViewModel {
        return EventLoggerViewModel(storeModule.eventLogStore())
    }
}