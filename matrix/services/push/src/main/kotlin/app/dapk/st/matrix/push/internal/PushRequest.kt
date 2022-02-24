package app.dapk.st.matrix.push.internal

import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest.Companion.httpRequest
import app.dapk.st.matrix.http.jsonBody
import app.dapk.st.matrix.push.PushService

fun registerPushRequest(pushRequest: PushService.PushRequest) = httpRequest<Unit>(
    path = "_matrix/client/r0/pushers/set",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(PushService.PushRequest.serializer(), pushRequest, MatrixHttpClient.jsonWithDefaults),
)