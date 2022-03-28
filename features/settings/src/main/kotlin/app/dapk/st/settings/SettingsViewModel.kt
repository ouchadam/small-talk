package app.dapk.st.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.Lce
import app.dapk.st.core.log
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.matrix.crypto.CryptoService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.settings.SettingItem.Id.*
import app.dapk.st.settings.SettingsEvent.*
import app.dapk.st.viewmodel.DapkViewModel
import app.dapk.st.viewmodel.MutableStateFactory
import app.dapk.st.viewmodel.defaultStateFactory
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://ouchadam.github.io/small-talk/privacy/"

internal class SettingsViewModel(
    private val cacheCleaner: StoreCleaner,
    private val contentResolver: ContentResolver,
    private val cryptoService: CryptoService,
    private val syncService: SyncService,
    private val uriFilenameResolver: UriFilenameResolver,
    private val settingsItemFactory: SettingsItemFactory,
    factory: MutableStateFactory<SettingsScreenState> = defaultStateFactory(),
) : DapkViewModel<SettingsScreenState, SettingsEvent>(
    initialState = SettingsScreenState(SpiderPage(Page.Routes.root, "Settings", null, Page.Root(Lce.Loading()))),
    factory = factory,
) {

    fun start() {
        viewModelScope.launch {
            val root = Page.Root(Lce.Content(settingsItemFactory.root()))
            val rootPage = SpiderPage(Page.Routes.root, "Settings", null, root)
            updateState { copy(page = rootPage) }
            println("state updated")
        }
    }

    fun goTo(page: SpiderPage<out Page>) {
        updateState { copy(page = page) }
    }

    fun onClick(item: SettingItem) {
        when (item.id) {
            SignOut -> {
                viewModelScope.launch {
                    log(AppLogTag.ERROR_NON_FATAL, "Sign out triggered")
                    cacheCleaner.cleanCache(removeCredentials = true)
                    _events.emit(SignedOut)
                }
            }
            AccessToken -> {
                viewModelScope.launch {
                    require(item is SettingItem.AccessToken)
                    _events.emit(CopyToClipboard("Token copied", item.accessToken))
                }
            }
            ClearCache -> {
                viewModelScope.launch {
                    cacheCleaner.cleanCache(removeCredentials = false)
                    _events.emit(Toast(message = "Cache deleted"))
                }
            }
            EventLog -> {
                viewModelScope.launch {
                    _events.emit(OpenEventLog)
                }
            }
            Encryption -> {
                updateState {
                    copy(page = SpiderPage(Page.Routes.encryption, "Encryption", Page.Routes.root, Page.Security))
                }
            }
            PrivacyPolicy -> {
                viewModelScope.launch {
                    _events.emit(OpenUrl(PRIVACY_POLICY_URL))
                }
            }
            Ignored -> {
                // do nothing
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
                onSuccess = { updatePageState<Page.ImportRoomKey> { copy(importProgress = Lce.Content(Unit)) } },
                onFailure = { updatePageState<Page.ImportRoomKey> { copy(importProgress = Lce.Error(it)) } }
            )
        }
    }

    fun goToImportRoom() {
        goTo(SpiderPage(Page.Routes.importRoomKeys, "Import room keys", Page.Routes.encryption, Page.ImportRoomKey()))
    }

    fun fileSelected(file: Uri) {
        viewModelScope.launch {
            val namedFile = NamedUri(
                name = uriFilenameResolver.readFilenameFromUri(file),
                uri = file
            )
            updatePageState<Page.ImportRoomKey> { copy(selectedFile = namedFile) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified S : Page> updatePageState(crossinline block: S.() -> S) {
        val page = state.page
        val currentState = page.state
        require(currentState is S)
        updateState { copy(page = (page as SpiderPage<S>).copy(state = block(page.state))) }
    }
}
