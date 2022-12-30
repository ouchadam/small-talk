package app.dapk.st.directory

import android.content.Context
import app.dapk.st.core.JobBag
import app.dapk.st.core.ProvidableModule
import app.dapk.st.directory.state.DirectoryEvent
import app.dapk.st.directory.state.DirectoryState
import app.dapk.st.directory.state.directoryReducer
import app.dapk.st.engine.ChatEngine
import app.dapk.st.state.createStateViewModel

class DirectoryModule(
    private val context: Context,
    private val chatEngine: ChatEngine,
) : ProvidableModule {

    fun directoryState(): DirectoryState {
        return createStateViewModel { directoryReducer(it) }
    }

    fun directoryReducer(eventEmitter: suspend (DirectoryEvent) -> Unit) = directoryReducer(chatEngine, ShortcutHandler(context), JobBag(), eventEmitter)
}
