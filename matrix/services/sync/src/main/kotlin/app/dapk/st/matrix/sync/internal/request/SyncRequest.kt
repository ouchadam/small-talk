package app.dapk.st.matrix.sync.internal.request

import app.dapk.st.matrix.common.SyncToken
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest.Companion.httpRequest
import app.dapk.st.matrix.http.queryMap
import app.dapk.st.matrix.sync.SyncService.FilterId

internal fun syncRequest(lastSyncToken: SyncToken?, filterId: FilterId?, timeoutMs: Long) =
    httpRequest<ApiSyncResponse>(
        path = "_matrix/client/r0/sync?${
            queryMap(
                "since" to lastSyncToken?.value,
                "filter" to filterId?.value,
                "timeout" to timeoutMs.toString(),
            )
        }",
        method = MatrixHttpClient.Method.GET,
    )