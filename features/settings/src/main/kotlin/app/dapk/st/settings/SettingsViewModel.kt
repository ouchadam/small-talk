package app.dapk.st.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.dapk.st.core.Lce
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.matrix.crypto.CryptoService
import app.dapk.st.matrix.crypto.ImportResult
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.push.PushTokenRegistrars
import app.dapk.st.push.Registrar
import app.dapk.st.settings.SettingItem.Id.*
import app.dapk.st.settings.SettingsEvent.*
import app.dapk.st.viewmodel.DapkViewModel
import app.dapk.st.viewmodel.MutableStateFactory
import app.dapk.st.viewmodel.defaultStateFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://ouchadam.github.io/small-talk/privacy/"

internal class SettingsViewModel(
    private val cacheCleaner: StoreCleaner,
    private val contentResolver: ContentResolver,
    private val cryptoService: CryptoService,
    private val syncService: SyncService,
    private val uriFilenameResolver: UriFilenameResolver,
    private val settingsItemFactory: SettingsItemFactory,
    private val pushTokenRegistrars: PushTokenRegistrars,
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
        }
    }

    fun goTo(page: SpiderPage<out Page>) {
        updateState { copy(page = page) }
    }

    fun onClick(item: SettingItem) {
        when (item.id) {
            SignOut -> {
                viewModelScope.launch {
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

            PushProvider -> {
                updateState {
                    copy(page = SpiderPage(Page.Routes.pushProviders, "Push providers", Page.Routes.root, Page.PushProviders()))
                }
            }

            Ignored -> {
                // do nothing
            }
        }
    }

    fun fetchPushProviders() {
        updatePageState<Page.PushProviders> { copy(options = Lce.Loading()) }
        viewModelScope.launch {
            val currentSelection = pushTokenRegistrars.currentSelection()
            val options = pushTokenRegistrars.options()
            updatePageState<Page.PushProviders> {
                copy(
                    selection = currentSelection,
                    options = Lce.Content(options)
                )
            }
        }
    }

    fun selectPushProvider(registrar: Registrar) {
        viewModelScope.launch {
            pushTokenRegistrars.makeSelection(registrar)
            fetchPushProviders()
        }
    }

    fun importFromFileKeys(file: Uri, passphrase: String) {
        updatePageState<Page.ImportRoomKey> { copy(importProgress = ImportResult.Update(0)) }
        viewModelScope.launch {
            with(cryptoService) {
                runCatching { contentResolver.openInputStream(file)!! }
                    .fold(
                        onSuccess = { fileStream ->
                            fileStream.importRoomKeys(passphrase)
                                .onEach {
                                    updatePageState<Page.ImportRoomKey> { copy(importProgress = it) }
                                    when (it) {
                                        is ImportResult.Error -> {
                                            // do nothing
                                        }
                                        is ImportResult.Update -> {
                                            // do nothing
                                        }
                                        is ImportResult.Success -> {
                                            syncService.forceManualRefresh(it.roomIds.toList())
                                        }
                                    }
                                }
                                .launchIn(viewModelScope)
                        },
                        onFailure = {
                            updatePageState<Page.ImportRoomKey> { copy(importProgress = ImportResult.Error(ImportResult.Error.Type.UnableToOpenFile)) }
                        }
                    )
            }
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
