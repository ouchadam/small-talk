package app.dapk.st.olm

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.common.extensions.toJsonString
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.internal.DeviceKeys
import org.matrix.olm.OlmAccount

class DeviceKeyFactory(
    private val jsonCanonicalizer: JsonCanonicalizer,
) {

    fun create(userId: UserId, deviceId: DeviceId, identityKey: Ed25519, senderKey: Curve25519, olmAccount: OlmAccount): DeviceKeys {
        val signable = mapOf(
            "device_id" to deviceId.value,
            "user_id" to userId.value,
            "algorithms" to listOf(Olm.ALGORITHM_MEGOLM.value, Olm.ALGORITHM_OLM.value),
            "keys" to mapOf(
                "curve25519:${deviceId.value}" to senderKey.value,
                "ed25519:${deviceId.value}" to identityKey.value,
            )
        ).toJsonString()

        return DeviceKeys(
            userId,
            deviceId,
            algorithms = listOf(Olm.ALGORITHM_MEGOLM, Olm.ALGORITHM_OLM),
            keys = mapOf(
                "curve25519:${deviceId.value}" to senderKey.value,
                "ed25519:${deviceId.value}" to identityKey.value,
            ),
            signatures = mapOf(
                userId.value to mapOf(
                    "ed25519:${deviceId.value}" to olmAccount.signMessage(jsonCanonicalizer.canonicalize(signable))
                )
            )
        )
    }
}