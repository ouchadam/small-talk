package app.dapk.st.settings

import app.dapk.st.core.*
import app.dapk.st.push.PushTokenRegistrars

internal class SettingsItemFactory(
    private val buildMeta: BuildMeta,
    private val deviceMeta: DeviceMeta,
    private val pushTokenRegistrars: PushTokenRegistrars,
    private val themeStore: ThemeStore,
) {

    suspend fun root() = general() + theme() + data() + account() + about()

    private suspend fun general() = listOf(
        SettingItem.Header("General"),
        SettingItem.Text(SettingItem.Id.Encryption, "Encryption"),
        SettingItem.Text(SettingItem.Id.EventLog, "Event log"),
        SettingItem.Text(SettingItem.Id.PushProvider, "Push provider", pushTokenRegistrars.currentSelection().id)
    )

    private fun theme() = listOfNotNull(
        SettingItem.Header("Theme"),
        SettingItem.Toggle(SettingItem.Id.ToggleDynamicTheme, "Enable Material You", state = themeStore.isMaterialYouEnabled()).takeIf {
            deviceMeta.isAtLeastS()
        },
    ).takeIf { it.size > 1 } ?: emptyList()

    private fun data() = listOf(
        SettingItem.Header("Data"),
        SettingItem.Text(SettingItem.Id.ClearCache, "Clear cache"),
    )

    private fun account() = listOf(
        SettingItem.Header("Account"),
        SettingItem.Text(SettingItem.Id.SignOut, "Sign out"),
    )

    private fun about() = listOf(
        SettingItem.Header("About"),
        SettingItem.Text(SettingItem.Id.PrivacyPolicy, "Privacy policy"),
        SettingItem.Text(SettingItem.Id.Ignored, "Version", buildMeta.versionName),
    )

}
