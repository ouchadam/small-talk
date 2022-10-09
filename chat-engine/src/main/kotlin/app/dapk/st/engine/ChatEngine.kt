package app.dapk.st.engine

import kotlinx.coroutines.flow.Flow

interface ChatEngine {

    fun directory(): Flow<DirectoryState>
    fun invites(): Flow<InviteState>

    suspend fun login(request: LoginRequest): LoginResult

    suspend fun me(forceRefresh: Boolean): Me

}
