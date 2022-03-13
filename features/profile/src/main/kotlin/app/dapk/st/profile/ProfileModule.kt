package app.dapk.st.profile

import app.dapk.st.core.ProvidableModule
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.SyncService

class ProfileModule(
    private val profileService: ProfileService,
    private val syncService: SyncService,
    private val roomService: RoomService,
) : ProvidableModule {

    fun profileViewModel(): ProfileViewModel {
        return ProfileViewModel(profileService, syncService, roomService)
    }

}