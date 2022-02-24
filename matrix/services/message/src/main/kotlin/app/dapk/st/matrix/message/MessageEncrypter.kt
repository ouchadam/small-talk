package app.dapk.st.matrix.message

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.CipherText
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.SessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun interface MessageEncrypter {

    suspend fun encrypt(message: MessageService.Message): EncryptedMessagePayload

    @Serializable
    data class EncryptedMessagePayload(
        @SerialName("algorithm") val algorithmName: AlgorithmName,
        @SerialName("sender_key") val senderKey: String,
        @SerialName("ciphertext") val cipherText: CipherText,
        @SerialName("session_id") val sessionId: SessionId,
        @SerialName("device_id") val deviceId: DeviceId
    )
}

internal object MissingMessageEncrypter : MessageEncrypter {
    override suspend fun encrypt(message: MessageService.Message) = throw IllegalStateException("No encrypter instance set")
}