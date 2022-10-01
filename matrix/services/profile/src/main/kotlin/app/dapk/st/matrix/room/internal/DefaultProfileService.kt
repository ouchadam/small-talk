package app.dapk.st.matrix.room.internal

import app.dapk.st.core.SingletonFlows
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.room.ProfileStore
import kotlinx.coroutines.flow.first

internal class DefaultProfileService(
    private val profileStore: ProfileStore,
    private val singletonFlows: SingletonFlows,
    private val fetchMeUseCase: FetchMeUseCase,
) : ProfileService {

    override suspend fun me(forceRefresh: Boolean): ProfileService.Me {
        return when (forceRefresh) {
            true -> fetchMe().also { profileStore.storeMe(it) }
            false -> singletonFlows.getOrPut("me") {
                profileStore.readMe() ?: fetchMe().also { profileStore.storeMe(it) }
            }.first()
        }
    }

    private suspend fun fetchMe(): ProfileService.Me {
        return fetchMeUseCase.fetchMe()
    }
}

