package app.dapk.st.settings

import app.dapk.st.core.Lce
import app.dapk.st.engine.ImportResult
import app.dapk.st.push.Registrar
import app.dapk.st.settings.state.ComponentLifecycle
import app.dapk.st.settings.state.RootActions
import app.dapk.st.settings.state.ScreenAction
import app.dapk.st.settings.state.settingsReducer
import app.dapk.state.Combined2
import app.dapk.state.SpiderPage
import app.dapk.state.page.PageAction
import app.dapk.state.page.PageContainer
import app.dapk.state.page.PageStateChange
import fake.*
import fixture.aRoomId
import internalfake.FakeSettingsItemFactory
import internalfake.FakeUriFilenameResolver
import internalfixture.aImportRoomKeysPage
import internalfixture.aPushProvidersPage
import internalfixture.aSettingTextItem
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import test.*

private const val APP_PRIVACY_POLICY_URL = "https://ouchadam.github.io/small-talk/privacy/"
private val A_LIST_OF_ROOT_ITEMS = listOf(aSettingTextItem())
private val A_URI = FakeUri()
private const val A_FILENAME = "a-filename.jpg"
private val AN_INITIAL_IMPORT_ROOM_KEYS_PAGE = aImportRoomKeysPage()
private val AN_INITIAL_PUSH_PROVIDERS_PAGE = aPushProvidersPage()
private val A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION = aImportRoomKeysPage(
    state = Page.ImportRoomKey(selectedFile = NamedUri(A_FILENAME, A_URI.instance))
)
private val A_LIST_OF_ROOM_IDS = listOf(aRoomId())
private val AN_IMPORT_SUCCESS = ImportResult.Success(A_LIST_OF_ROOM_IDS.toSet(), totalImportedKeysCount = 5)
private val AN_IMPORT_FILE_ERROR = ImportResult.Error(ImportResult.Error.Type.UnableToOpenFile)
private val AN_INPUT_STREAM = FakeInputStream()
private const val A_PASSPHRASE = "passphrase"
private val AN_ERROR = RuntimeException()
private val A_REGISTRAR = Registrar("a-registrar-id")
private val A_PUSH_OPTIONS = listOf(Registrar("a-registrar-id"))

internal class SettingsReducerTest {

    private val fakeStoreCleaner = FakeStoreCleaner()
    private val fakeContentResolver = FakeContentResolver()
    private val fakeUriFilenameResolver = FakeUriFilenameResolver()
    private val fakePushTokenRegistrars = FakePushRegistrars()
    private val fakeSettingsItemFactory = FakeSettingsItemFactory()
    private val fakeThemeStore = FakeThemeStore()
    private val fakeLoggingStore = FakeLoggingStore()
    private val fakeMessageOptionsStore = FakeMessageOptionsStore()
    private val fakeChatEngine = FakeChatEngine()
    private val fakeJobBag = FakeJobBag()

    private val runReducerTest = testReducer { fakeEventSource ->
        settingsReducer(
            fakeChatEngine,
            fakeStoreCleaner,
            fakeContentResolver.instance,
            fakeUriFilenameResolver.instance,
            fakeSettingsItemFactory.instance,
            fakePushTokenRegistrars.instance,
            fakeThemeStore.instance,
            fakeLoggingStore.instance,
            fakeMessageOptionsStore.instance,
            fakeEventSource,
            fakeJobBag.instance,
        )
    }

    @Test
    fun `initial state is root with loading`() = runReducerTest {
        assertInitialState(
            pageState(SpiderPage(Page.Routes.root, "Settings", null, Page.Root(Lce.Loading())))
        )
    }

    @Test
    fun `given root content, when Visible, then goes to root page with content`() = runReducerTest {
        fakeSettingsItemFactory.givenRoot().returns(A_LIST_OF_ROOT_ITEMS)
        fakeJobBag.instance.expect { it.replace("page", any()) }

        reduce(ComponentLifecycle.Visible)

        assertOnlyDispatches(
            PageAction.GoTo(
                SpiderPage(
                    Page.Routes.root,
                    "Settings",
                    null,
                    Page.Root(Lce.Content(A_LIST_OF_ROOT_ITEMS))
                )
            )
        )
    }

    @Test
    fun `when SelectPushProvider, then selects provider and refreshes`() = runReducerTest {
        fakePushTokenRegistrars.instance.expect { it.makeSelection(A_REGISTRAR) }

        reduce(RootActions.SelectPushProvider(A_REGISTRAR))

        assertOnlyDispatches(RootActions.FetchProviders)
    }

    @Test
    fun `when FetchProviders, then selects provider and refreshes`() = runReducerTest {
        setState(pageState(aPushProvidersPage()))
        fakePushTokenRegistrars.givenOptions().returns(A_PUSH_OPTIONS)
        fakePushTokenRegistrars.givenCurrentSelection().returns(A_REGISTRAR)

        reduce(RootActions.FetchProviders)

        assertOnlyDispatches(
            PageStateChange.UpdatePage(
                aPushProvidersPage().state.copy(options = Lce.Loading())
            ),
            PageStateChange.UpdatePage(
                aPushProvidersPage().state.copy(
                    selection = A_REGISTRAR,
                    options = Lce.Content(A_PUSH_OPTIONS)
                )
            )
        )
    }

    @Test
    fun `when SelectKeysFile, then updates ImportRoomKey page with file`() = runReducerTest {
        setState(pageState(AN_INITIAL_IMPORT_ROOM_KEYS_PAGE))
        fakeUriFilenameResolver.givenFilename(A_URI.instance).returns(A_FILENAME)

        reduce(RootActions.SelectKeysFile(A_URI.instance))

        assertOnlyDispatches(
            PageStateChange.UpdatePage(
                AN_INITIAL_IMPORT_ROOM_KEYS_PAGE.state.copy(
                    selectedFile = NamedUri(A_FILENAME, A_URI.instance)
                )
            )
        )
    }

    @Test
    fun `when Click SignOut, then clears store and signs out`() = runReducerTest {
        fakeStoreCleaner.expectUnit { it.cleanCache(removeCredentials = true) }
        val aSignOutItem = aSettingTextItem(id = SettingItem.Id.SignOut)

        reduce(ScreenAction.OnClick(aSignOutItem))

        assertEvents(SettingsEvent.SignedOut)
    }

    @Test
    fun `when Click Encryption, then goes to Encryption page`() = runReducerTest {
        val anEncryptionItem = aSettingTextItem(id = SettingItem.Id.Encryption)

        reduce(ScreenAction.OnClick(anEncryptionItem))

        assertOnlyDispatches(
            PageAction.GoTo(
                SpiderPage(
                    Page.Routes.encryption,
                    "Encryption",
                    Page.Routes.root,
                    Page.Security
                )
            )
        )
    }

    @Test
    fun `when Click PrivacyPolicy, then opens privacy policy url`() = runReducerTest {
        val aPrivacyPolicyItem = aSettingTextItem(id = SettingItem.Id.PrivacyPolicy)

        reduce(ScreenAction.OnClick(aPrivacyPolicyItem))

        assertOnlyEvents(SettingsEvent.OpenUrl(APP_PRIVACY_POLICY_URL))
    }

    @Test
    fun `when Click PushProvider, then goes to PushProvider page`() = runReducerTest {
        val aPushProviderItem = aSettingTextItem(id = SettingItem.Id.PushProvider)

        reduce(ScreenAction.OnClick(aPushProviderItem))

        assertOnlyDispatches(PageAction.GoTo(aPushProvidersPage()))
    }

    @Test
    fun `when Click Ignored, then does nothing`() = runReducerTest {
        val anIgnoredItem = aSettingTextItem(id = SettingItem.Id.Ignored)

        reduce(ScreenAction.OnClick(anIgnoredItem))

        assertNoChanges()
    }

    @Test
    fun `when Click ToggleDynamicTheme, then toggles flag, recreates activity and reloads`() = runReducerTest {
        val aToggleThemeItem = aSettingTextItem(id = SettingItem.Id.ToggleDynamicTheme)
        fakeThemeStore.givenMaterialYouIsEnabled().returns(true)
        fakeThemeStore.instance.expect { it.storeMaterialYouEnabled(false) }

        reduce(ScreenAction.OnClick(aToggleThemeItem))

        assertEvents(SettingsEvent.RecreateActivity)
        assertDispatches(ComponentLifecycle.Visible)
        assertNoStateChange()
    }

    @Test
    fun `when Click ToggleEnableLogs, then toggles flag and reloads`() = runReducerTest {
        val aToggleEnableLogsItem = aSettingTextItem(id = SettingItem.Id.ToggleEnableLogs)
        fakeLoggingStore.givenLoggingIsEnabled().returns(true)
        fakeLoggingStore.instance.expect { it.setEnabled(false) }

        reduce(ScreenAction.OnClick(aToggleEnableLogsItem))

        assertOnlyDispatches(ComponentLifecycle.Visible)
    }

    @Test
    fun `when Click EventLog, then opens event log`() = runReducerTest {
        val anEventLogItem = aSettingTextItem(id = SettingItem.Id.EventLog)

        reduce(ScreenAction.OnClick(anEventLogItem))

        assertOnlyEvents(SettingsEvent.OpenEventLog)
    }

    @Test
    fun `when Click ToggleSendReadReceipts, then toggles flag and reloads`() = runReducerTest {
        val aToggleReadReceiptsItem = aSettingTextItem(id = SettingItem.Id.ToggleSendReadReceipts)
        fakeMessageOptionsStore.givenReadReceiptsDisabled().returns(true)
        fakeMessageOptionsStore.instance.expect { it.setReadReceiptsDisabled(false) }

        reduce(ScreenAction.OnClick(aToggleReadReceiptsItem))

        assertOnlyDispatches(ComponentLifecycle.Visible)
    }

    @Test
    fun `given success, when ImportKeysFromFile, then dispatches progress`() = runReducerTest {
        setState(pageState(A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION))
        fakeContentResolver.givenFile(A_URI.instance).returns(AN_INPUT_STREAM.instance)
        fakeChatEngine.givenImportKeys(AN_INPUT_STREAM.instance, A_PASSPHRASE).returns(flowOf(AN_IMPORT_SUCCESS))

        reduce(RootActions.ImportKeysFromFile(A_URI.instance, A_PASSPHRASE))

        assertOnlyDispatches(
            PageStateChange.UpdatePage(
                A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION.state.copy(importProgress = ImportResult.Update(0L))
            ),
            PageStateChange.UpdatePage(
                A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION.state.copy(importProgress = AN_IMPORT_SUCCESS)
            ),
        )
    }

    @Test
    fun `given error, when ImportKeysFromFile, then dispatches error`() = runReducerTest {
        setState(pageState(A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION))
        fakeContentResolver.givenFile(A_URI.instance).throws(AN_ERROR)

        reduce(RootActions.ImportKeysFromFile(A_URI.instance, A_PASSPHRASE))

        assertOnlyDispatches(
            PageStateChange.UpdatePage(
                A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION.state.copy(importProgress = ImportResult.Update(0L))
            ),
            PageStateChange.UpdatePage(
                A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION.state.copy(importProgress = AN_IMPORT_FILE_ERROR)
            ),
        )
    }

}

private fun <P> pageState(page: SpiderPage<out P>) = Combined2(PageContainer(page), Unit)
