package app.dapk.st.settings.state

import android.net.Uri
import app.dapk.st.push.Registrar
import app.dapk.st.settings.SettingItem
import app.dapk.state.Action

internal sealed interface ScreenAction : Action {
    data class OnClick(val item: SettingItem) : ScreenAction
    object OpenImportRoom : ScreenAction
}

internal sealed interface RootActions : Action {
    object FetchProviders : RootActions
    data class SelectPushProvider(val registrar: Registrar) : RootActions
    data class ImportKeysFromFile(val file: Uri, val passphrase: String) : RootActions
    data class SelectKeysFile(val file: Uri) : RootActions
}

internal sealed interface ComponentLifecycle : Action {
    object Visible : ComponentLifecycle
}
