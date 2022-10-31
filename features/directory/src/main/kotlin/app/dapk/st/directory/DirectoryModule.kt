package app.dapk.st.directory

import android.content.Context
import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.StateViewModel
import app.dapk.st.core.createStateViewModel
import app.dapk.st.directory.state.DirectoryEvent
import app.dapk.st.directory.state.DirectoryScreenState
import app.dapk.st.directory.state.directoryReducer
import app.dapk.st.engine.ChatEngine

class DirectoryModule(
    private val context: Context,
    private val chatEngine: ChatEngine,
) : ProvidableModule {

    internal fun directoryViewModel(): StateViewModel<DirectoryScreenState, DirectoryEvent> {
        return createStateViewModel { directoryReducer(chatEngine, ShortcutHandler(context), it) }
    }
}
