package app.dapk.st.matrix.push.internal

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.push.PushService

class DefaultPushService(
    httpClient: MatrixHttpClient,
    credentialsStore: CredentialsStore,
    logger: MatrixLogger,
) : PushService {

    private val useCase = RegisterPushUseCase(httpClient, credentialsStore, logger)

    override suspend fun registerPush(token: String, gatewayUrl: String) {
        useCase.registerPushToken(token, gatewayUrl)
    }

}