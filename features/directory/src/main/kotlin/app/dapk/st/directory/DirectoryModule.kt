package app.dapk.st.directory

import android.content.Context
import app.dapk.st.core.JobBag
import app.dapk.st.core.ProvidableModule
import app.dapk.st.directory.state.DirectoryEvent
import app.dapk.st.directory.state.directoryReducer
import app.dapk.st.engine.ChatEngine
import app.dapk.st.imageloader.IconLoader

class DirectoryModule(
    private val context: Context,
    private val chatEngine: ChatEngine,
    private val iconLoader: IconLoader,
) : ProvidableModule {

    fun directoryReducer(eventEmitter: suspend (DirectoryEvent) -> Unit) = directoryReducer(chatEngine, shortcutHandler(), JobBag(), eventEmitter)

    private fun shortcutHandler() = ShortcutHandler(context, iconLoader)
}
