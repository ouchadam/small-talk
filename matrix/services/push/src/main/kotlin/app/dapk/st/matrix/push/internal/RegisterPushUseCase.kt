package app.dapk.st.matrix.push.internal

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.isSignedIn
import app.dapk.st.matrix.common.matrixLog
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.push.PushService.PushRequest

internal class RegisterPushUseCase(
    private val matrixClient: MatrixHttpClient,
    private val credentialsStore: CredentialsStore,
    private val logger: MatrixLogger,
) {

    suspend fun registerPushToken(token: String) {
        if (credentialsStore.isSignedIn()) {
            logger.matrixLog("register push token: $token")
            matrixClient.execute(
                registerPushRequest(
                    PushRequest(
                        pushKey = token,
                        kind = "http",
                        appId = "app.dapk.st",
                        appDisplayName = "st-android",
                        deviceDisplayName = "device-a",
                        lang = "en",
                        profileTag = "mobile_${credentialsStore.credentials()!!.userId.hashCode()}",
                        append = false,
                        data = PushRequest.Payload(
                            format = "event_id_only",
                            url = "https://sygnal.dapk.app/_matrix/push/v1/notify",
                        ),
                    )
                )
            )
        }
    }
}