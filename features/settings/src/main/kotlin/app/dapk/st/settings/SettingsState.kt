package app.dapk.st.settings

import android.net.Uri
import app.dapk.st.core.Lce
import app.dapk.st.engine.ImportResult
import app.dapk.st.push.Registrar
import app.dapk.state.Route
import app.dapk.state.SpiderPage

internal data class SettingsScreenState(
    val page: SpiderPage<out Page>,
)

internal sealed interface Page {
    data class Root(val content: Lce<List<SettingItem>>) : Page
    object Security : Page
    data class ImportRoomKey(
        val selectedFile: NamedUri? = null,
        val importProgress: ImportResult? = null,
    ) : Page

    data class PushProviders(
        val selection: Registrar? = null,
        val options: Lce<List<Registrar>>? = Lce.Loading()
    ) : Page

    object Routes {
        val root = Route<Root>("Settings")
        val encryption = Route<Security>("Encryption")
        val pushProviders = Route<PushProviders>("PushProviders")
        val importRoomKeys = Route<ImportRoomKey>("ImportRoomKey")
    }
}

data class NamedUri(
    val name: String?,
    val uri: Uri,
)

internal sealed interface SettingItem {

    val id: Id

    data class Header(val label: String, override val id: Id = Id.Ignored) : SettingItem
    data class Text(override val id: Id, val content: String, val subtitle: String? = null, val enabled: Boolean = true) : SettingItem
    data class Toggle(override val id: Id, val content: String, val subtitle: String? = null, val state: Boolean) : SettingItem
    data class AccessToken(override val id: Id, val content: String, val accessToken: String) : SettingItem

    enum class Id {
        SignOut,
        AccessToken,
        ClearCache,
        EventLog,
        PushProvider,
        Encryption,
        PrivacyPolicy,
        Ignored,
        ToggleDynamicTheme,
        ToggleEnableLogs,
        ToggleSendReadReceipts,
    }
}

sealed interface SettingsEvent {

    object SignedOut : SettingsEvent
    data class Toast(val message: String) : SettingsEvent
    object OpenEventLog : SettingsEvent
    data class OpenUrl(val url: String) : SettingsEvent
    data class CopyToClipboard(val message: String, val content: String) : SettingsEvent
    object RecreateActivity : SettingsEvent
}

