package app.dapk.st.settings

import app.dapk.st.core.BuildMeta
import internalfixture.aSettingHeaderItem
import internalfixture.aSettingTextItem
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class SettingsItemFactoryTest {

    private val buildMeta = BuildMeta(versionName = "a-version-name", versionCode = 100)

    private val settingsItemFactory = SettingsItemFactory(buildMeta, )

    @Test
    fun `when creating root items, then is expected`() {
        val result = settingsItemFactory.root()

        result shouldBeEqualTo listOf(
            aSettingHeaderItem("General"),
            aSettingTextItem(SettingItem.Id.Encryption, "Encryption"),
            aSettingTextItem(SettingItem.Id.EventLog, "Event log"),
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