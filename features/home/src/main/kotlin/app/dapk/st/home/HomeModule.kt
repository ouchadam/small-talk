package app.dapk.st.home

import app.dapk.st.core.ProvidableModule
import app.dapk.st.directory.state.DirectoryState
import app.dapk.st.domain.StoreModule
import app.dapk.st.engine.ChatEngine
import app.dapk.st.login.LoginViewModel
import app.dapk.st.profile.ProfileViewModel

class HomeModule(
    private val chatEngine: ChatEngine,
    private val storeModule: StoreModule,
    val betaVersionUpgradeUseCase: BetaVersionUpgradeUseCase,
) : ProvidableModule {

    internal fun homeViewModel(directory: DirectoryState, login: LoginViewModel, profileViewModel: ProfileViewModel): HomeViewModel {
        return HomeViewModel(
            chatEngine,
            storeModule.credentialsStore(),
            directory,
            login,
            profileViewModel,
            storeModule.cacheCleaner(),
            betaVersionUpgradeUseCase,
        )
    }

}