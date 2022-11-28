package app.dapk.st.directory

import android.content.Context
import app.dapk.st.core.ProvidableModule
import app.dapk.st.state.createStateViewModel
import app.dapk.st.core.JobBag
import app.dapk.st.directory.state.DirectoryState
import app.dapk.st.directory.state.directoryReducer
import app.dapk.st.engine.ChatEngine

class DirectoryModule(
    private val context: Context,
    private val chatEngine: ChatEngine,
) : ProvidableModule {

    fun directoryState(): DirectoryState {
        return createStateViewModel { directoryReducer(chatEngine, ShortcutHandler(context), JobBag(), it) }
    }
}
