package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.common.DeviceCredentials
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest.Companion.httpRequest
import app.dapk.st.matrix.http.emptyJsonBody
import app.dapk.st.matrix.http.jsonBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun loginRequest(userId: UserId, password: String, baseUrl: String, deviceDisplayName: String?) = httpRequest<ApiAuthResponse>(
    path = "_matrix/client/r0/login",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(
        PasswordLoginRequest.serializer(),
        PasswordLoginRequest(PasswordLoginRequest.UserIdentifier(userId), password, deviceDisplayName),
        MatrixHttpClient.jsonWithDefaults
    ),
    authenticated = false,
    baseUrl = baseUrl,
)

fun registerStartFlowRequest(baseUrl: String) = httpRequest<Unit>(
    path = "_matrix/client/r0/register",
    method = MatrixHttpClient.Method.POST,
    body = emptyJsonBody(),
    authenticated = false,
    baseUrl = baseUrl,
)

internal fun registerRequest(userName: String, password: String, baseUrl: String, auth: Auth?) = httpRequest<ApiAuthResponse>(
    path = "_matrix/client/r0/register",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(
        PasswordRegisterRequest(userName, password, auth?.let { PasswordRegisterRequest.Auth(it.session, it.type) }),
        MatrixHttpClient.jsonWithDefaults
    ),
    authenticated = false,
    baseUrl = baseUrl,
)

internal fun wellKnownRequest(baseUrl: String) = httpRequest<RawResponse>(
    path = ".well-known/matrix/client",
    method = MatrixHttpClient.Method.GET,
    baseUrl = baseUrl,
    authenticated = false,
)

typealias RawResponse = ByteArray

fun RawResponse.readString() = this.toString(Charsets.UTF_8)

internal data class Auth(
    val session: String,
    val type: String,
)

@Serializable
data class ApiAuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("home_server") val homeServer: String,
    @SerialName("user_id") override val userId: UserId,
    @SerialName("device_id") override val deviceId: DeviceId,
    @SerialName("well_known") val wellKnown: ApiWellKnown? = null,
) : DeviceCredentials

@Serializable
data class ApiWellKnown(
    @SerialName("m.homeserver") val homeServer: HomeServer
) {
    @Serializable
    data class HomeServer(
        @SerialName("base_url") val baseUrl: HomeServerUrl,
    )
}

@Serializable
internal data class PasswordLoginRequest(
    @SerialName("identifier") val userName: UserIdentifier,
    @SerialName("password") val password: String,
    @SerialName("initial_device_display_name") val deviceDisplayName: String?,
    @SerialName("type") val type: String = "m.login.password",
) {

    @Serializable
    internal data class UserIdentifier(
        @SerialName("user") val userName: UserId,
        @SerialName("type") val type: String = "m.id.user",
    )
}

@Serializable
internal data class PasswordRegisterRequest(
    @SerialName("username") val userName: String,
    @SerialName("password") val password: String,
    @SerialName("auth") val auth: Auth?,
) {
    @Serializable
    data class Auth(
        @SerialName("session") val session: String,
        @SerialName("type") val type: String,
    )
}
