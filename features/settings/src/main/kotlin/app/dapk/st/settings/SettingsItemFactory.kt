package app.dapk.st.settings

import app.dapk.st.core.BuildMeta
import app.dapk.st.core.ThemeStore
import app.dapk.st.push.PushTokenRegistrars

internal class SettingsItemFactory(
    private val buildMeta: BuildMeta,
    private val pushTokenRegistrars: PushTokenRegistrars,
    private val themeStore: ThemeStore,
) {

    suspend fun root() = listOf(
        SettingItem.Header("General"),
        SettingItem.Text(SettingItem.Id.Encryption, "Encryption"),
        SettingItem.Text(SettingItem.Id.EventLog, "Event log"),
        SettingItem.Text(SettingItem.Id.PushProvider, "Push provider", pushTokenRegistrars.currentSelection().id),
        SettingItem.Header("Theme"),
        SettingItem.Toggle(SettingItem.Id.ToggleDynamicTheme, "Enable Material You", state = themeStore.isMaterialYouEnabled()),
        SettingItem.Header("Data"),
        SettingItem.Text(SettingItem.Id.ClearCache, "Clear cache"),
        SettingItem.Header("Account"),
        SettingItem.Text(SettingItem.Id.SignOut, "Sign out"),
        SettingItem.Header("About"),
        SettingItem.Text(SettingItem.Id.PrivacyPolicy, "Privacy policy"),
        SettingItem.Text(SettingItem.Id.Ignored, "Version", buildMeta.versionName),
    )

}
