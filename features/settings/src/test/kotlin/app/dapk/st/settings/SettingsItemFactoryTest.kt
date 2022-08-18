package app.dapk.st.settings

import app.dapk.st.core.BuildMeta
import app.dapk.st.push.PushTokenRegistrars
import app.dapk.st.push.Registrar
import internalfixture.aSettingHeaderItem
import internalfixture.aSettingTextItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn

private val A_SELECTION = Registrar("A_SELECTION")

class SettingsItemFactoryTest {

    private val buildMeta = BuildMeta(versionName = "a-version-name", versionCode = 100)
    private val fakePushTokenRegistrars = FakePushRegistrars()

    private val settingsItemFactory = SettingsItemFactory(buildMeta, fakePushTokenRegistrars.instance)

    @Test
    fun `when creating root items, then is expected`() = runTest {
        fakePushTokenRegistrars.givenCurrentSelection().returns(A_SELECTION)

        val result = settingsItemFactory.root()

        result shouldBeEqualTo listOf(
            aSettingHeaderItem("General"),
            aSettingTextItem(SettingItem.Id.Encryption, "Encryption"),
            aSettingTextItem(SettingItem.Id.EventLog, "Event log"),
            aSettingTextItem(SettingItem.Id.PushProvider, "Push provider", A_SELECTION.id),
            aSettingHeaderItem("Data"),
            aSettingTextItem(SettingItem.Id.ClearCache, "Clear cache"),
            aSettingHeaderItem("Account"),
            aSettingTextItem(SettingItem.Id.SignOut, "Sign out"),
            aSettingHeaderItem("About"),
            aSettingTextItem(SettingItem.Id.PrivacyPolicy, "Privacy policy"),
            aSettingTextItem(SettingItem.Id.Ignored, "Version", buildMeta.versionName),
        )
    }
}

class FakePushRegistrars {

    val instance = mockk<PushTokenRegistrars>()

    fun givenCurrentSelection() = coEvery { instance.currentSelection() }.delegateReturn()

}