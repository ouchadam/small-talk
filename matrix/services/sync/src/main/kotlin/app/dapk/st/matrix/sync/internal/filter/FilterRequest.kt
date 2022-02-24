package app.dapk.st.matrix.sync.internal.filter

import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest.Companion.httpRequest
import app.dapk.st.matrix.http.jsonBody
import app.dapk.st.matrix.sync.internal.request.ApiFilterResponse
import app.dapk.st.matrix.sync.internal.request.FilterRequest

internal fun filterRequest(userId: UserId, filterRequest: FilterRequest) = httpRequest<ApiFilterResponse>(
    path = "_matrix/client/r0/user/${userId.value}/filter",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(FilterRequest.serializer(), filterRequest),
)
