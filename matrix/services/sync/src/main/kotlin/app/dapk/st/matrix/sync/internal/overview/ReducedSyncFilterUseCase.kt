package app.dapk.st.matrix.sync.internal.overview

import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.sync.SyncService.FilterId
import app.dapk.st.matrix.sync.internal.filter.FilterUseCase
import app.dapk.st.matrix.sync.internal.request.EventFilter
import app.dapk.st.matrix.sync.internal.request.FilterRequest
import app.dapk.st.matrix.sync.internal.request.RoomEventFilter
import app.dapk.st.matrix.sync.internal.request.RoomFilter

private const val FIlTER_KEY = "reduced-filter-key"

internal class ReducedSyncFilterUseCase(
    private val filterUseCase: FilterUseCase,
) {

    suspend fun reducedFilter(userId: UserId): FilterId {
        return filterUseCase.filter(
            key = FIlTER_KEY,
            userId = userId,
            filterRequest = reduced()
        )
    }

}

private fun reduced() = FilterRequest(
    roomFilter = RoomFilter(
        timelineFilter = RoomEventFilter(
            lazyLoadMembers = true,
        ),
        stateFilter = RoomEventFilter(
            lazyLoadMembers = true,
        ),
        ephemeralFilter = RoomEventFilter(types = listOf("m.typing")),
        accountFilter = RoomEventFilter(types = listOf("m.fully_read")),
    ),
    account = EventFilter(types = listOf("m.direct")),
)
