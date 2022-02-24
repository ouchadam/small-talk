package app.dapk.st.olm

import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.Ed25519
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.crypto.Olm
import org.matrix.olm.OlmSAS
import org.matrix.olm.OlmUtility

internal class DefaultSasSession(private val selfFingerprint: Ed25519) : Olm.SasSession {

    private val olmSAS = OlmSAS()

    override fun publicKey(): String {
        return olmSAS.publicKey
    }

    override suspend fun generateCommitment(hash: String, startJsonString: String): String {
        val utility = OlmUtility()
        return utility.sha256(olmSAS.publicKey + startJsonString).also {
            utility.releaseUtility()
        }
    }

    override suspend fun calculateMac(
        selfUserId: UserId,
        selfDeviceId: DeviceId,
        otherUserId: UserId,
        otherDeviceId: DeviceId,
        transactionId: String
    ): Olm.MacResult {
        val baseInfo = "MATRIX_KEY_VERIFICATION_MAC" +
                selfUserId.value +
                selfDeviceId.value +
                otherUserId.value +
                otherDeviceId.value +
                transactionId
        val deviceKeyId = "ed25519:${selfDeviceId.value}"
        val macMap = mapOf(
            deviceKeyId to olmSAS.calculateMac(selfFingerprint.value, baseInfo + deviceKeyId)
        )
        val keys = olmSAS.calculateMac(macMap.keys.sorted().joinToString(separator = ","), baseInfo + "KEY_IDS")
        return Olm.MacResult(macMap, keys)
    }

    override fun setTheirPublicKey(key: String) {
        olmSAS.setTheirPublicKey(key)
    }

    override fun release() {
        olmSAS.releaseSas()
    }
}