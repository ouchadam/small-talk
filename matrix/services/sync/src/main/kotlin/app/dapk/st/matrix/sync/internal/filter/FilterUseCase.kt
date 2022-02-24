package app.dapk.st.matrix.sync.internal.filter

import app.dapk.st.core.extensions.ifNull
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.sync.FilterStore
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.matrix.sync.internal.request.FilterRequest

internal class FilterUseCase(
    private val client: MatrixHttpClient,
    private val filterStore: FilterStore,
) {

    suspend fun filter(key: String, userId: UserId, filterRequest: FilterRequest): SyncService.FilterId {
        val filterId = filterStore.read(key).ifNull {
            client.execute(filterRequest(userId, filterRequest)).id.also {
                filterStore.store(key, it)
            }
        }
        return SyncService.FilterId(filterId)
    }

}