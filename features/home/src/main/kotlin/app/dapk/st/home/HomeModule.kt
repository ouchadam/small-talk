package app.dapk.st.home

import app.dapk.st.core.BuildMeta
import app.dapk.st.core.ProvidableModule
import app.dapk.st.directory.DirectoryViewModel
import app.dapk.st.domain.StoreModule
import app.dapk.st.login.LoginViewModel
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.profile.ProfileViewModel

class HomeModule(
    private val storeModule: StoreModule,
    private val profileService: ProfileService,
    private val syncService: SyncService,
    private val buildMeta: BuildMeta,
) : ProvidableModule {

    fun homeViewModel(directory: DirectoryViewModel, login: LoginViewModel, profileViewModel: ProfileViewModel): HomeViewModel {
        return HomeViewModel(
            storeModule.credentialsStore(),
            directory,
            login,
            profileViewModel,
            profileService,
            storeModule.cacheCleaner(),
            BetaVersionUpgradeUseCase(
                storeModule.applicationStore(),
                buildMeta,
            ),
            syncService,
        )
    }

}