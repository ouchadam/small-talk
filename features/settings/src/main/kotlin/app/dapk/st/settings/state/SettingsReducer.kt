package app.dapk.st.settings.state

import android.content.ContentResolver
import app.dapk.st.core.JobBag
import app.dapk.st.core.Lce
import app.dapk.st.core.State
import app.dapk.st.core.ThemeStore
import app.dapk.st.core.page.PageAction
import app.dapk.st.core.page.PageContainer
import app.dapk.st.core.page.createPageReducer
import app.dapk.st.core.page.withPageContext
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.domain.application.eventlog.LoggingStore
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.engine.ChatEngine
import app.dapk.st.engine.ImportResult
import app.dapk.st.push.PushTokenRegistrars
import app.dapk.st.settings.*
import app.dapk.st.settings.SettingItem.Id.*
import app.dapk.st.settings.SettingsEvent.*
import app.dapk.state.Combined2
import app.dapk.state.async
import app.dapk.state.createReducer
import app.dapk.state.multi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://ouchadam.github.io/small-talk/privacy/"

internal fun settingsReducer(
    chatEngine: ChatEngine,
    cacheCleaner: StoreCleaner,
    contentResolver: ContentResolver,
    uriFilenameResolver: UriFilenameResolver,
    settingsItemFactory: SettingsItemFactory,
    pushTokenRegistrars: PushTokenRegistrars,
    themeStore: ThemeStore,
    loggingStore: LoggingStore,
    messageOptionsStore: MessageOptionsStore,
    eventEmitter: suspend (SettingsEvent) -> Unit,
    jobBag: JobBag,
) = createPageReducer(
    initialPage = SpiderPage<Page>(Page.Routes.root, "Settings", null, Page.Root(Lce.Loading())),
    factory = {
        createReducer(
            initialState = Unit,

            async(ComponentLifecycle.Visible::class) {
                jobBag.replace("page", coroutineScope.launch {
                    val root = Page.Root(Lce.Content(settingsItemFactory.root()))
                    val rootPage = SpiderPage(Page.Routes.root, "Settings", null, root)
                    dispatch(PageAction.GoTo(rootPage))
                })
            },

            async(RootActions.FetchProviders::class) {
                withPageContext<Page.PushProviders> {
                    pageDispatch(PageAction.UpdatePage(it.copy(options = Lce.Loading())))
                }

                val currentSelection = pushTokenRegistrars.currentSelection()
                val options = pushTokenRegistrars.options()
                withPageContext<Page.PushProviders> {
                    pageDispatch(
                        PageAction.UpdatePage(
                            it.copy(
                                selection = currentSelection,
                                options = Lce.Content(options)
                            )
                        )
                    )
                }
            },

            async(RootActions.SelectPushProvider::class) {
                pushTokenRegistrars.makeSelection(it.registrar)
                dispatch(RootActions.FetchProviders)
            },


            async(RootActions.ImportKeysFromFile::class) { action ->
                withPageContext<Page.ImportRoomKey> {
                    pageDispatch(PageAction.UpdatePage(it.copy(importProgress = ImportResult.Update(0))))
                }

                with(chatEngine) {
                    runCatching { contentResolver.openInputStream(action.file)!! }
                        .fold(
                            onSuccess = { fileStream ->
                                fileStream.importRoomKeys(action.passphrase)
                                    .onEach { progress ->
                                        withPageContext<Page.ImportRoomKey> {
                                            pageDispatch(PageAction.UpdatePage(it.copy(importProgress = progress)))
                                        }
                                    }
                                    .launchIn(coroutineScope)
                            },
                            onFailure = {

                                withPageContext<Page.ImportRoomKey> {
                                    pageDispatch(PageAction.UpdatePage(it.copy(importProgress = ImportResult.Error(ImportResult.Error.Type.UnableToOpenFile))))
                                }
                            }
                        )
                }
            },

            async(RootActions.SelectKeysFile::class) { action ->
                val namedFile = NamedUri(
                    name = uriFilenameResolver.readFilenameFromUri(action.file),
                    uri = action.file
                )

                withPageContext<Page.ImportRoomKey> {
                    pageDispatch(PageAction.UpdatePage(it.copy(selectedFile = namedFile)))
                }
            },

            multi(ScreenAction.OnClick::class) { action ->
                val item = action.item
                when (item.id) {
                    SignOut -> sideEffect {
                        cacheCleaner.cleanCache(removeCredentials = true)
                        eventEmitter.invoke(SignedOut)
                    }

                    AccessToken -> sideEffect {
                        require(item is SettingItem.AccessToken)
                        eventEmitter.invoke(CopyToClipboard("Token copied", item.accessToken))
                    }

                    ClearCache -> sideEffect {
                        cacheCleaner.cleanCache(removeCredentials = false)
                        eventEmitter.invoke(Toast(message = "Cache deleted"))
                    }

                    EventLog -> sideEffect {
                        eventEmitter.invoke(OpenEventLog)
                    }

                    Encryption -> async {
                        dispatch(PageAction.GoTo(SpiderPage(Page.Routes.encryption, "Encryption", Page.Routes.root, Page.Security)))
                    }

                    PrivacyPolicy -> sideEffect {
                        eventEmitter.invoke(OpenUrl(PRIVACY_POLICY_URL))
                    }

                    PushProvider -> async {
                        dispatch(PageAction.GoTo(SpiderPage(Page.Routes.pushProviders, "Push providers", Page.Routes.root, Page.PushProviders())))
                    }

                    Ignored -> {
                        nothing()
                    }

                    ToggleDynamicTheme -> async {
                        themeStore.storeMaterialYouEnabled(!themeStore.isMaterialYouEnabled())
                        dispatch(ComponentLifecycle.Visible)
                        eventEmitter.invoke(RecreateActivity)
                    }

                    ToggleEnableLogs -> async {
                        loggingStore.setEnabled(!loggingStore.isEnabled())
                        dispatch(ComponentLifecycle.Visible)
                    }

                    ToggleSendReadReceipts -> async {
                        messageOptionsStore.setReadReceiptsDisabled(!messageOptionsStore.isReadReceiptsDisabled())
                        dispatch(ComponentLifecycle.Visible)
                    }
                }
            },

            async(ScreenAction.OpenImportRoom::class) {
                dispatch(PageAction.GoTo(SpiderPage(Page.Routes.importRoomKeys, "Import room keys", Page.Routes.encryption, Page.ImportRoomKey())))
            },
        )
    }
)


internal typealias SettingsState = State<Combined2<PageContainer<Page>, Unit>, SettingsEvent>