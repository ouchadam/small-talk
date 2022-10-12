package app.dapk.st.directory

import android.content.Context
import app.dapk.st.core.ProvidableModule
import app.dapk.st.engine.ChatEngine

class DirectoryModule(
    private val context: Context,
    private val chatEngine: ChatEngine,
) : ProvidableModule {

    fun directoryViewModel(): DirectoryViewModel {
        return DirectoryViewModel(
            ShortcutHandler(context),
            chatEngine,
        )
    }
}