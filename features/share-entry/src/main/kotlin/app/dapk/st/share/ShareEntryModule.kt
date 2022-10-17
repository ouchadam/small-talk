package app.dapk.st.share

import app.dapk.st.core.ProvidableModule
import app.dapk.st.engine.ChatEngine

class ShareEntryModule(
    private val chatEngine: ChatEngine,
) : ProvidableModule {

    fun shareEntryViewModel(): ShareEntryViewModel {
        return ShareEntryViewModel(FetchRoomsUseCase(chatEngine))
    }
}