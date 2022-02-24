package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.crypto
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.DeviceService.KeyClaim
import app.dapk.st.matrix.device.internal.ClaimKeysResponse
import app.dapk.st.matrix.device.internal.DeviceKeys
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val KEY_SIGNED_CURVE_25519_TYPE = AlgorithmName("signed_curve25519")

internal typealias RegisterOlmSessionUseCase = suspend (List<DeviceKeys>, Olm.AccountCryptoSession) -> List<Olm.DeviceCryptoSession>

internal class RegisterOlmSessionUseCaseImpl(
    private val olm: Olm,
    private val deviceService: DeviceService,
    private val logger: MatrixLogger,
) : RegisterOlmSessionUseCase {

    override suspend fun invoke(deviceKeys: List<DeviceKeys>, olmAccount: Olm.AccountCryptoSession): List<Olm.DeviceCryptoSession> {
        logger.crypto("registering olm session for devices")
        val devicesByDeviceId = deviceKeys.associateBy { it.deviceId }
        val keyClaims = deviceKeys.map { KeyClaim(it.userId, it.deviceId, algorithmName = KEY_SIGNED_CURVE_25519_TYPE) }
        logger.crypto("attempt claim: $keyClaims")
        return deviceService.claimKeys(keyClaims)
            .toOlmRequests(devicesByDeviceId)
            .also { logger.crypto("claim result: $it") }
            .map { olm.ensureDeviceCrypto(it, olmAccount) }
    }

    private fun ClaimKeysResponse.toOlmRequests(devices: Map<DeviceId, DeviceKeys>) = this.oneTimeKeys.map { (userId, devicesToKeys) ->
        devicesToKeys.mapNotNull { (deviceId, payload) ->
            when (payload) {
                is JsonObject -> {
                    val key = when (val content = payload.values.first()) {
                        is JsonObject -> (content["key"] as JsonPrimitive).content
                        else -> throw RuntimeException("Missing key")
                    }
                    val identity = devices.identity(deviceId)
                    val fingerprint = devices.fingerprint(deviceId)
                    Olm.OlmSessionInput(oneTimeKey = key, identity = identity, deviceId, userId, fingerprint)
                }
                else -> null
            }
        }
    }.flatten()
}

private fun Map<DeviceId, DeviceKeys>.identity(deviceId: DeviceId) = this[deviceId]!!.identity()
private fun Map<DeviceId, DeviceKeys>.fingerprint(deviceId: DeviceId) = this[deviceId]!!.fingerprint()