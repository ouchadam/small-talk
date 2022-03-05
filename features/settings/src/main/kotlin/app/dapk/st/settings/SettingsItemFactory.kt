package app.dapk.st.settings

import app.dapk.st.core.BuildMeta

internal class SettingsItemFactory(private val buildMeta: BuildMeta) {

    fun root() = listOf(
        SettingItem.Header("General"),
        SettingItem.Text(SettingItem.Id.Encryption, "Encryption"),
        SettingItem.Text(SettingItem.Id.EventLog, "Event log"),
        SettingItem.Header("Account"),
        SettingItem.Text(SettingItem.Id.SignOut, "Sign out"),
        SettingItem.Header("About"),
        SettingItem.Text(SettingItem.Id.PrivacyPolicy, "Privacy policy"),
        SettingItem.Text(SettingItem.Id.Ignored, "Version", buildMeta.versionName),
    )

}