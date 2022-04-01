package app.dapk.st.settings

import ViewModelTest
import app.dapk.st.core.Lce
import app.dapk.st.design.components.SpiderPage
import fake.*
import fixture.FakeStoreCleaner
import fixture.aRoomId
import internalfake.FakeSettingsItemFactory
import internalfake.FakeUriFilenameResolver
import internalfixture.aImportRoomKeysPage
import internalfixture.aSettingTextItem
import org.junit.Test

private const val APP_PRIVACY_POLICY_URL = "https://ouchadam.github.io/small-talk/privacy/"
private val A_LIST_OF_ROOT_ITEMS = listOf(aSettingTextItem())
private val A_URI = FakeUri()
private const val A_FILENAME = "a-filename.jpg"
private val AN_INITIAL_IMPORT_ROOM_KEYS_PAGE = aImportRoomKeysPage()
private val A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION = aImportRoomKeysPage(
    state = Page.ImportRoomKey(selectedFile = NamedUri(A_FILENAME, A_URI.instance))
)
private val A_LIST_OF_ROOM_IDS = listOf(aRoomId())
private val AN_INPUT_STREAM = FakeInputStream()
private const val A_PASSPHRASE = "passphrase"
private val AN_ERROR = RuntimeException()

internal class SettingsViewModelTest {

    private val runViewModelTest = ViewModelTest()

    private val fakeStoreCleaner = FakeStoreCleaner()
    private val fakeContentResolver = FakeContentResolver()
    private val fakeCryptoService = FakeCryptoService()
    private val fakeSyncService = FakeSyncService()
    private val fakeUriFilenameResolver = FakeUriFilenameResolver()
    private val fakeSettingsItemFactory = FakeSettingsItemFactory()

    private val viewModel = SettingsViewModel(
        fakeStoreCleaner,
        fakeContentResolver.instance,
        fakeCryptoService,
        fakeSyncService,
        fakeUriFilenameResolver.instance,
        fakeSettingsItemFactory.instance,
        runViewModelTest.testMutableStateFactory(),
    )

    @Test
    fun `when creating view model then initial state is loading Root`() = runViewModelTest {
        viewModel.test()

        assertInitialState(
            SettingsScreenState(SpiderPage(Page.Routes.root, "Settings", null, Page.Root(Lce.Loading())))
        )
    }

    @Test
    fun `when starting, then emits root page with content`() = runViewModelTest {
        fakeSettingsItemFactory.givenRoot().returns(A_LIST_OF_ROOT_ITEMS)

        viewModel.test().start()

        assertStates(
            SettingsScreenState(
                SpiderPage(
                    Page.Routes.root,
                    "Settings",
                    null,
                    Page.Root(Lce.Content(A_LIST_OF_ROOT_ITEMS))
                )
            )
        )
        assertNoEvents<SettingsEvent>()
    }

    @Test
    fun `when sign out clicked, then clears store`() = runViewModelTest {
        fakeStoreCleaner.expectUnit { it.cleanCache(removeCredentials = true) }
        val aSignOutItem = aSettingTextItem(id = SettingItem.Id.SignOut)

        viewModel.test().onClick(aSignOutItem)

        assertNoStates<SettingsScreenState>()
        assertEvents(SettingsEvent.SignedOut)
        verifyExpects()
    }

    @Test
    fun `when event log clicked, then opens event log`() = runViewModelTest {
        val anEventLogItem = aSettingTextItem(id = SettingItem.Id.EventLog)

        viewModel.test().onClick(anEventLogItem)

        assertNoStates<SettingsScreenState>()
        assertEvents(SettingsEvent.OpenEventLog)
    }

    @Test
    fun `when encryption clicked, then emits encryption page`() = runViewModelTest {
        val anEncryptionItem = aSettingTextItem(id = SettingItem.Id.Encryption)

        viewModel.test().onClick(anEncryptionItem)

        assertNoEvents<SettingsEvent>()
        assertStates(
            SettingsScreenState(
                SpiderPage(
                    route = Page.Routes.encryption,
                    label = "Encryption",
                    parent = Page.Routes.root,
                    state = Page.Security
                )
            )
        )
    }

    @Test
    fun `when privacy policy clicked, then opens privacy policy url`() = runViewModelTest {
        val aPrivacyPolicyItem = aSettingTextItem(id = SettingItem.Id.PrivacyPolicy)

        viewModel.test().onClick(aPrivacyPolicyItem)

        assertNoStates<SettingsScreenState>()
        assertEvents(SettingsEvent.OpenUrl(APP_PRIVACY_POLICY_URL))
    }

    @Test
    fun `when going to import room, then emits import room keys page`() = runViewModelTest {
        viewModel.test().goToImportRoom()

        assertStates(
            SettingsScreenState(
                SpiderPage(
                    route = Page.Routes.importRoomKeys,
                    label = "Import room keys",
                    parent = Page.Routes.encryption,
                    state = Page.ImportRoomKey()
                )
            )
        )
        assertNoEvents<SettingsEvent>()
    }

    @Test
    fun `given on import room keys page, when selecting file, then emits selection`() = runViewModelTest {
        fakeUriFilenameResolver.givenFilename(A_URI.instance).returns(A_FILENAME)

        viewModel.test(initialState = SettingsScreenState(AN_INITIAL_IMPORT_ROOM_KEYS_PAGE)).fileSelected(A_URI.instance)

        assertStates(
            SettingsScreenState(
                AN_INITIAL_IMPORT_ROOM_KEYS_PAGE.copy(
                    state = Page.ImportRoomKey(
                        selectedFile = NamedUri(A_FILENAME, A_URI.instance)
                    )
                )
            )
        )
        assertNoEvents<SettingsEvent>()
    }

    @Test
    fun `given success when importing room keys, then emits progress`() = runViewModelTest {
        fakeSyncService.expectUnit { it.forceManualRefresh(A_LIST_OF_ROOM_IDS) }
        fakeContentResolver.givenFile(A_URI.instance).returns(AN_INPUT_STREAM.instance)
        fakeCryptoService.givenImportKeys(AN_INPUT_STREAM.instance, A_PASSPHRASE).returns(A_LIST_OF_ROOM_IDS)

        viewModel
            .test(initialState = SettingsScreenState(A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION))
            .importFromFileKeys(A_URI.instance, A_PASSPHRASE)

        assertStates<SettingsScreenState>(
            { copy(page = page.updateState<Page.ImportRoomKey> { copy(importProgress = Lce.Loading()) }) },
            { copy(page = page.updateState<Page.ImportRoomKey> { copy(importProgress = Lce.Content(Unit)) }) },
        )
        assertNoEvents<SettingsEvent>()
        verifyExpects()
    }

    @Test
    fun `given error when importing room keys, then emits error`() = runViewModelTest {
        fakeContentResolver.givenFile(A_URI.instance).throws(AN_ERROR)

        viewModel
            .test(initialState = SettingsScreenState(A_IMPORT_ROOM_KEYS_PAGE_WITH_SELECTION))
            .importFromFileKeys(A_URI.instance, A_PASSPHRASE)

        assertStates<SettingsScreenState>(
            { copy(page = page.updateState<Page.ImportRoomKey> { copy(importProgress = Lce.Loading()) }) },
            { copy(page = page.updateState<Page.ImportRoomKey> { copy(importProgress = Lce.Error(AN_ERROR)) }) },
        )
        assertNoEvents<SettingsEvent>()
    }

}

@Suppress("UNCHECKED_CAST")
private inline fun <reified S : Page> SpiderPage<out Page>.updateState(crossinline block: S.() -> S): SpiderPage<Page> {
    require(this.state is S)
    return (this as SpiderPage<S>).copy(state = block(this.state)) as SpiderPage<Page>
}
