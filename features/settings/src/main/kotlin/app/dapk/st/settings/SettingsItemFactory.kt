package app.dapk.st.settings

import app.dapk.st.core.BuildMeta
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.ThemeStore
import app.dapk.st.core.isAtLeastS
import app.dapk.st.domain.application.eventlog.LoggingStore
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.push.PushTokenRegistrars

internal class SettingsItemFactory(
    private val buildMeta: BuildMeta,
    private val deviceMeta: DeviceMeta,
    private val pushTokenRegistrars: PushTokenRegistrars,
    private val themeStore: ThemeStore,
    private val loggingStore: LoggingStore,
    private val messageOptionsStore: MessageOptionsStore,
) {

    suspend fun root() = general() + theme() + data() + account() + advanced() + about()

    private suspend fun general() = listOf(
        SettingItem.Header("General"),
        SettingItem.Text(SettingItem.Id.Encryption, "Encryption"),
        SettingItem.Text(SettingItem.Id.PushProvider, "Push provider", pushTokenRegistrars.currentSelection().id)
    )

    private suspend fun theme() = listOfNotNull(
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

    private suspend fun advanced(): List<SettingItem> {
        val loggingIsEnabled = loggingStore.isEnabled()
        return listOf(
            SettingItem.Header("Advanced"),
            SettingItem.Toggle(
                SettingItem.Id.ToggleSendReadReceipts,
                "Don't send message read receipts",
                subtitle = "Requires the Homeserver to be running Synapse 1.65+",
                state = messageOptionsStore.isReadReceiptsDisabled()
            ),
            SettingItem.Toggle(SettingItem.Id.ToggleEnableLogs, "Enable local logging", state = loggingIsEnabled),
            SettingItem.Text(SettingItem.Id.EventLog, "Event log", enabled = loggingIsEnabled),
        )
    }

    private fun about() = listOf(
        SettingItem.Header("About"),
        SettingItem.Text(SettingItem.Id.PrivacyPolicy, "Privacy policy"),
        SettingItem.Text(SettingItem.Id.Ignored, "Version", buildMeta.versionName),
    )

}
