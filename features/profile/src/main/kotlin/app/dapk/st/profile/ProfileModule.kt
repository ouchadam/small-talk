package app.dapk.st.profile

import app.dapk.st.core.ProvidableModule
import app.dapk.st.matrix.room.ProfileService

class ProfileModule(
    private val profileService: ProfileService,
) : ProvidableModule {

    fun profileViewModel(): ProfileViewModel {
        return ProfileViewModel(profileService)
    }

}