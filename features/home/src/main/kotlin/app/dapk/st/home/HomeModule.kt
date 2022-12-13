package app.dapk.st.home

import app.dapk.st.core.ProvidableModule
import app.dapk.st.directory.state.DirectoryState
import app.dapk.st.domain.StoreModule
import app.dapk.st.engine.ChatEngine
import app.dapk.st.login.state.LoginState
import app.dapk.st.profile.state.ProfileState

class HomeModule(
    private val chatEngine: ChatEngine,
    private val storeModule: StoreModule,
    val betaVersionUpgradeUseCase: BetaVersionUpgradeUseCase,
) : ProvidableModule {

    internal fun homeViewModel(directory: DirectoryState, login: LoginState, profile: ProfileState): HomeViewModel {
        return HomeViewModel(
            chatEngine,
            directory,
            login,
            profile,
            storeModule.cacheCleaner(),
            betaVersionUpgradeUseCase,
        )
    }

}