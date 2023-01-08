package app.dapk.st.profile

import app.dapk.st.core.JobBag
import app.dapk.st.core.ProvidableModule
import app.dapk.st.state.createStateViewModel
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.engine.ChatEngine
import app.dapk.st.profile.state.ProfileState
import app.dapk.st.profile.state.ProfileUseCase
import app.dapk.st.profile.state.profileReducer

class ProfileModule(
    private val chatEngine: ChatEngine,
    private val errorTracker: ErrorTracker,
) : ProvidableModule {

    fun profileState(): ProfileState {
        return createStateViewModel { profileReducer() }
    }

    fun profileReducer() = profileReducer(chatEngine, errorTracker, ProfileUseCase(chatEngine, errorTracker), JobBag())

}