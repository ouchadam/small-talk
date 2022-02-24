package app.dapk.st.matrix.device.internal

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.device.ToDevicePayload
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest.Companion.httpRequest
import app.dapk.st.matrix.http.jsonBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

internal fun uploadKeysRequest(keyRequest: UploadKeyRequest) = httpRequest<UploadKeysResponse>(
    path = "_matrix/client/r0/keys/upload",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(keyRequest, MatrixHttpClient.jsonWithDefaults),
)

internal fun queryKeys(queryRequest: QueryKeysRequest) = httpRequest<QueryKeysResponse>(
    path = "_matrix/client/r0/keys/query",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(queryRequest, MatrixHttpClient.jsonWithDefaults),
)


internal fun claimKeys(claimRequest: ClaimKeysRequest) = httpRequest<ClaimKeysResponse>(
    path = "_matrix/client/r0/keys/claim",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(claimRequest, MatrixHttpClient.jsonWithDefaults),
)

internal fun sendToDeviceRequest(eventType: EventType, txnId: String, request: SendToDeviceRequest) = httpRequest<Unit>(
    path = "_matrix/client/r0/sendToDevice/${eventType.value}/${txnId}",
    method = MatrixHttpClient.Method.PUT,
    body = jsonBody(request)
)

@Serializable
internal data class UploadKeysResponse(
    @SerialName("one_time_key_counts") val keyCounts: Map<String, Int>
)

@Serializable
internal data class SendToDeviceRequest(
    @SerialName("messages") val messages: Map<UserId, Map<DeviceId, ToDevicePayload>>
)


@Serializable
internal data class UploadKeyRequest(
    @SerialName("device_keys") val deviceKeys: DeviceKeys? = null,
    @SerialName("one_time_keys") val oneTimeKeys: Map<String, JsonElement>? = null,
)

@Serializable
internal data class QueryKeysRequest(
    @SerialName("timeout") val timeout: Int = 10000,
    @SerialName("device_keys") val deviceKeys: Map<UserId, List<DeviceId>>,
    @SerialName("token") val token: String? = null,
)

@Serializable
internal data class QueryKeysResponse(
    @SerialName("device_keys") val deviceKeys: Map<UserId, Map<DeviceId, DeviceKeys>>
)

@Serializable
internal data class ClaimKeysRequest(
    @SerialName("timeout") val timeout: Int = 10000,
    @SerialName("one_time_keys") val oneTimeKeys: Map<UserId, Map<DeviceId, AlgorithmName>>,
)

@Serializable
data class ClaimKeysResponse(
    @SerialName("one_time_keys") val oneTimeKeys: Map<UserId, Map<DeviceId, JsonElement>>,
    @SerialName("failures") val failures: Map<String, JsonElement>
)

@Serializable
data class DeviceKeys(
    @SerialName("user_id") val userId: UserId,
    @SerialName("device_id") val deviceId: DeviceId,
    @SerialName("algorithms") val algorithms: List<AlgorithmName>,
    @SerialName("keys") val keys: Map<String, String>,
    @SerialName("signatures") val signatures: Map<String, Map<String, String>>,
) {
    fun fingerprint() = Ed25519(keys["ed25519:${deviceId.value}"]!!)
    fun identity() = Curve25519(keys["curve25519:${deviceId.value}"]!!)
}