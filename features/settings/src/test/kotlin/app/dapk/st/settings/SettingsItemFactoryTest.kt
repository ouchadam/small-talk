package app.dapk.st.settings

import app.dapk.st.core.BuildMeta
import app.dapk.st.core.DeviceMeta
import app.dapk.st.push.PushTokenRegistrars
import app.dapk.st.push.Registrar
import fake.FakeLoggingStore
import fake.FakeMessageOptionsStore
import internalfixture.aSettingHeaderItem
import internalfixture.aSettingTextItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn

private val A_SELECTION = Registrar("A_SELECTION")
private const val ENABLED_MATERIAL_YOU = true
private const val DISABLED_LOGGING = false
private const val DISABLED_READ_RECEIPTS = true

class SettingsItemFactoryTest {

    private val buildMeta = BuildMeta(versionName = "a-version-name", versionCode = 100, isDebug = false)
    private val deviceMeta = DeviceMeta(apiVersion = 31)
    private val fakePushTokenRegistrars = FakePushRegistrars()
    private val fakeThemeStore = FakeThemeStore()
    private val fakeLoggingStore = FakeLoggingStore()
    private val fakeMessageOptionsStore = FakeMessageOptionsStore()

    private val settingsItemFactory = SettingsItemFactory(
        buildMeta,
        deviceMeta,
        fakePushTokenRegistrars.instance,
        fakeThemeStore.instance,
        fakeLoggingStore.instance,
        fakeMessageOptionsStore.instance,
    )

    @Test
    fun `when creating root items, then is expected`() = runTest {
        fakePushTokenRegistrars.givenCurrentSelection().returns(A_SELECTION)
        fakeThemeStore.givenMaterialYouIsEnabled().returns(ENABLED_MATERIAL_YOU)
        fakeLoggingStore.givenLoggingIsEnabled().returns(DISABLED_LOGGING)
        fakeMessageOptionsStore.givenReadReceiptsDisabled().returns(DISABLED_READ_RECEIPTS)

        val result = settingsItemFactory.root()

        result shouldBeEqualTo listOf(
            aSettingHeaderItem("General"),
            aSettingTextItem(SettingItem.Id.Encryption, "Encryption"),
            aSettingTextItem(SettingItem.Id.PushProvider, "Push provider", A_SELECTION.id),
            SettingItem.Header("Theme"),
            SettingItem.Toggle(SettingItem.Id.ToggleDynamicTheme, "Enable Material You", state = ENABLED_MATERIAL_YOU),
            aSettingHeaderItem("Data"),
            aSettingTextItem(SettingItem.Id.ClearCache, "Clear cache"),
            aSettingHeaderItem("Account"),
            aSettingTextItem(SettingItem.Id.SignOut, "Sign out"),
            aSettingHeaderItem("Advanced"),
            SettingItem.Toggle(
                SettingItem.Id.ToggleSendReadReceipts,
                "Don't send message read receipts",
                subtitle = "Requires the Homeserver to be running Synapse 1.65+",
                state = DISABLED_READ_RECEIPTS
            ),
            SettingItem.Toggle(SettingItem.Id.ToggleEnableLogs, "Enable local logging", state = DISABLED_LOGGING),
            aSettingTextItem(SettingItem.Id.EventLog, "Event log", enabled = DISABLED_LOGGING),
            aSettingHeaderItem("About"),
            aSettingTextItem(SettingItem.Id.PrivacyPolicy, "Privacy policy"),
            aSettingTextItem(SettingItem.Id.Ignored, "Version", buildMeta.versionName),
        )
    }
}

class FakePushRegistrars {

    val instance = mockk<PushTokenRegistrars>()

    fun givenCurrentSelection() = coEvery { instance.currentSelection() }.delegateReturn()
    fun givenOptions() = coEvery { instance.options() }.delegateReturn()

}