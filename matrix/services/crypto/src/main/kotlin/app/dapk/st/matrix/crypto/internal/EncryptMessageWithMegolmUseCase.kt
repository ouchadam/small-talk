package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.DeviceCredentials
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.crypto
import app.dapk.st.matrix.crypto.Crypto
import app.dapk.st.matrix.crypto.Olm

private val ALGORITHM_MEGOLM = AlgorithmName("m.megolm.v1.aes-sha2")

internal typealias EncryptMessageWithMegolmUseCase = suspend (DeviceCredentials, MessageToEncrypt) -> Crypto.EncryptionResult

internal class EncryptMessageWithMegolmUseCaseImpl(
    private val olm: Olm,
    private val fetchMegolmSessionUseCase: FetchMegolmSessionUseCase,
    private val logger: MatrixLogger,
) : EncryptMessageWithMegolmUseCase {

    override suspend fun invoke(credentials: DeviceCredentials, message: MessageToEncrypt): Crypto.EncryptionResult {
        logger.crypto("encrypt")
        val roomSession = fetchMegolmSessionUseCase.invoke(message.roomId)
        val encryptedMessage = with(olm) { roomSession.encrypt(message.roomId, message.json) }
        return Crypto.EncryptionResult(
            ALGORITHM_MEGOLM,
            senderKey = roomSession.accountCryptoSession.senderKey.value,
            cipherText = encryptedMessage,
            sessionId = roomSession.id,
            deviceId = credentials.deviceId
        )
    }

}
