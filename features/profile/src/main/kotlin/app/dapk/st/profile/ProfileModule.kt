package app.dapk.st.profile

import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.engine.ChatEngine

class ProfileModule(
    private val chatEngine: ChatEngine,
    private val errorTracker: ErrorTracker,
) : ProvidableModule {

    fun profileViewModel(): ProfileViewModel {
        return ProfileViewModel(chatEngine, errorTracker)
    }

}