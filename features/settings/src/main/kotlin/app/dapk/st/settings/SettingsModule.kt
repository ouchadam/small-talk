package app.dapk.st.settings

import android.content.ContentResolver
import app.dapk.st.core.BuildMeta
import app.dapk.st.core.ProvidableModule
import app.dapk.st.domain.StoreModule
import app.dapk.st.matrix.crypto.CryptoService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.settings.eventlogger.EventLoggerViewModel

class SettingsModule(
    private val storeModule: StoreModule,
    private val cryptoService: CryptoService,
    private val syncService: SyncService,
    private val contentResolver: ContentResolver,
    private val buildMeta: BuildMeta,
) : ProvidableModule {

    fun settingsViewModel() = SettingsViewModel(
        storeModule.credentialsStore(),
        storeModule.cacheCleaner(),
        contentResolver,
        cryptoService,
        syncService,
        UriFilenameResolver(contentResolver),
        buildMeta
    )

    fun eventLogViewModel(): EventLoggerViewModel {
        return EventLoggerViewModel(storeModule.eventLogStore())
    }
}