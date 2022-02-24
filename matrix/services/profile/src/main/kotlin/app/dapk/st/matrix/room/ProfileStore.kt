package app.dapk.st.matrix.room

interface ProfileStore {

    suspend fun storeMe(me: ProfileService.Me)
    suspend fun readMe(): ProfileService.Me?

}