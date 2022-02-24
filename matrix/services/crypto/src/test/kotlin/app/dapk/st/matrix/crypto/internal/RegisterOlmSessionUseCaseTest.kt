package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.crypto.Olm
import fake.FakeDeviceService
import fake.FakeMatrixLogger
import fake.FakeOlm
import fixture.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val KEY_SIGNED_CURVE_25519_TYPE = AlgorithmName("signed_curve25519")

private const val A_CLAIM_KEY_RESPONSE = "a-claimed-key"
private const val A_DEVICE_IDENTITY = "a-claimed-signature"
private const val A_DEVICE_FINGERPRINT = "a-claimed-fingerprint"
private val A_DEVICE_ID_TO_REGISTER = aDeviceId("device-id-to-register")
private val A_USER_ID_TO_REGISTER = aUserId("user-id-to-register")
private val A_DEVICE_KEYS_TO_REGISTER = aDeviceKeys(
    userId = A_USER_ID_TO_REGISTER,
    deviceId = A_DEVICE_ID_TO_REGISTER,
    keys = mapOf(
        "ed25519:${A_DEVICE_ID_TO_REGISTER.value}" to A_DEVICE_FINGERPRINT,
        "curve25519:${A_DEVICE_ID_TO_REGISTER.value}" to A_DEVICE_IDENTITY,
    )
)
private val A_DEVICE_CRYPTO_SESSION = aDeviceCryptoSession(identity = aCurve25519("an-olm-identity"))
private val A_KEY_CLAIM = aKeyClaim(
    userId = A_USER_ID_TO_REGISTER,
    deviceId = A_DEVICE_ID_TO_REGISTER,
    algorithmName = KEY_SIGNED_CURVE_25519_TYPE
)
private val AN_ACCOUNT_CRYPTO_SESSION = anAccountCryptoSession()

class RegisterOlmSessionUseCaseTest {

    private val fakeOlm = FakeOlm()
    private val fakeDeviceService = FakeDeviceService()

    private val registerOlmSessionUseCase = RegisterOlmSessionUseCaseImpl(
        fakeOlm,
        fakeDeviceService,
        FakeMatrixLogger()
    )

    @Test
    fun `given keys when registering then claims keys and creates olm session`() = runTest {
        fakeDeviceService.givenClaimsKeys(listOf(A_KEY_CLAIM)).returns(claimKeysResponse(A_USER_ID_TO_REGISTER, A_DEVICE_ID_TO_REGISTER))
        val expectedInput = expectOlmSessionCreationInput()
        fakeOlm.givenDeviceCrypto(expectedInput, AN_ACCOUNT_CRYPTO_SESSION).returns(A_DEVICE_CRYPTO_SESSION)

        val result = registerOlmSessionUseCase.invoke(listOf(A_DEVICE_KEYS_TO_REGISTER), AN_ACCOUNT_CRYPTO_SESSION)

        result shouldBeEqualTo listOf(A_DEVICE_CRYPTO_SESSION)
    }

    private fun expectOlmSessionCreationInput() = Olm.OlmSessionInput(
        A_CLAIM_KEY_RESPONSE,
        A_DEVICE_KEYS_TO_REGISTER.identity(),
        A_DEVICE_ID_TO_REGISTER,
        A_USER_ID_TO_REGISTER,
        A_DEVICE_KEYS_TO_REGISTER.fingerprint()
    )

    private fun claimKeysResponse(userId: UserId, deviceId: DeviceId) = aClaimKeysResponse(oneTimeKeys = mapOf(userId to mapOf(deviceId to jsonElement())))

    private fun jsonElement() = Json.encodeToJsonElement(
        JsonObject(
            mapOf(
                "signed_curve25519:AAAAHg" to JsonObject(
                    mapOf(
                        "key" to JsonPrimitive(A_CLAIM_KEY_RESPONSE),
                        "signatures" to JsonObject(
                            mapOf(
                                A_USER_ID_TO_REGISTER.value to JsonObject(
                                    mapOf("ed25519:${A_DEVICE_ID_TO_REGISTER.value}" to JsonPrimitive(A_DEVICE_FINGERPRINT))
                                )
                            )
                        )
                    )
                )
            )
        )
    )
}
