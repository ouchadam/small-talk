package app.dapk.st.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.dapk.st.core.BuildMeta
import app.dapk.st.core.DapkViewModel
import app.dapk.st.core.Lce
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.crypto.CryptoService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.settings.SettingItem.Id.*
import app.dapk.st.settings.SettingsEvent.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val credentialsStore: CredentialsStore,
    private val cacheCleaner: StoreCleaner,
    private val contentResolver: ContentResolver,
    private val cryptoService: CryptoService,
    private val syncService: SyncService,
    private val uriFilenameResolver: UriFilenameResolver,
    private val buildMeta: BuildMeta,
) : DapkViewModel<SettingsScreenState, SettingsEvent>(
    initialState = SettingsScreenState(SpiderPage(Page.Routes.root, "Settings", null, Page.Root(Lce.Loading())))
) {

    fun start() {
        viewModelScope.launch {
            val root = Page.Root(
                Lce.Content(
                    listOf(
                        SettingItem.Header("General"),
                        SettingItem.Text(Encryption, "Encryption"),
                        SettingItem.Text(EventLog, "Event log"),
                        SettingItem.Header("Account"),
                        SettingItem.Text(SignOut, "Sign out"),
                        SettingItem.Header("About"),
                        SettingItem.Text(Ignored, "Version", buildMeta.versionName),
                    )
                )
            )
            val rootPage = SpiderPage(Page.Routes.root, "Settings", null, root)
            updateState { copy(page = rootPage) }
        }
    }

    fun goTo(page: SpiderPage<out Page>) {
        updateState { copy(page = page) }
    }

    fun onClick(item: SettingItem) {
        when (item.id) {
            SignOut -> {
                viewModelScope.launch { credentialsStore.clear() }
                _events.tryEmit(SignedOut)
            }
            AccessToken -> {
                require(item is SettingItem.AccessToken)
                _events.tryEmit(CopyToClipboard("Token copied", item.accessToken))
            }
            ClearCache -> {
                viewModelScope.launch {
                    cacheCleaner.cleanCache(removeCredentials = false)
                    _events.tryEmit(Toast(message = "Cache deleted"))
                }
            }
            EventLog -> {
                _events.tryEmit(OpenEventLog)
            }
            Encryption -> {
                updateState {
                    copy(page = SpiderPage(Page.Routes.encryption, "Encryption", Page.Routes.root, Page.Security))
                }
            }
        }
    }

    fun importFromFileKeys(file: Uri, passphrase: String) {
        updatePageState<Page.ImportRoomKey> { copy(importProgress = Lce.Loading()) }
        viewModelScope.launch {
            kotlin.runCatching {
                with(cryptoService) {
                    val roomsToRefresh = contentResolver.openInputStream(file)?.importRoomKeys(passphrase)
                    roomsToRefresh?.let { syncService.forceManualRefresh(roomsToRefresh) }
                }
            }.fold(
                onSuccess = { updatePageState<Page.ImportRoomKey> { copy(importProgress = Lce.Content(true)) } },
                onFailure = { updatePageState<Page.ImportRoomKey> { copy(importProgress = Lce.Error(it)) } }
            )
        }
    }

    fun goToImportRoom() {
        updateState {
            copy(page = SpiderPage(Page.Routes.importRoomKeys, "Import room keys", Page.Routes.encryption, Page.ImportRoomKey()))
        }
    }

    fun fileSelected(file: Uri) {
        val namedFile = NamedUri(
            name = uriFilenameResolver.readFilenameFromUri(file),
            uri = file
        )
        updatePageState<Page.ImportRoomKey> { copy(selectedFile = namedFile) }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified S : Page> updatePageState(crossinline block: S.() -> S) {
        val page = state.page
        val currentState = page.state
        require(currentState is S)
        updateState { copy(page = (page as SpiderPage<S>).copy(state = block(page.state))) }
    }
}
