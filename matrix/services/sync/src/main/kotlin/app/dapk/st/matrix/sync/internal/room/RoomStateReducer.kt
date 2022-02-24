package app.dapk.st.matrix.sync.internal.room

import app.dapk.st.matrix.common.DecryptionResult
import app.dapk.st.matrix.common.EncryptedMessageContent
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.sync.internal.request.ApiEncryptedContent

internal fun ApiEncryptedContent.export(senderId: UserId): EncryptedMessageContent? {
    return when (this) {
        is ApiEncryptedContent.MegOlmV1 -> EncryptedMessageContent.MegOlmV1(
            this.cipherText, this.deviceId, this.senderKey, this.sessionId
        )
        is ApiEncryptedContent.OlmV1 -> EncryptedMessageContent.OlmV1(
            senderId = senderId,
            this.cipherText.mapValues { EncryptedMessageContent.CipherTextInfo(it.value.body, it.value.type) },
            this.senderKey
        )
        ApiEncryptedContent.Unknown -> null
    }
}

fun interface MessageDecrypter {
    suspend fun decrypt(event: EncryptedMessageContent): DecryptionResult
}

internal object MissingMessageDecrypter : MessageDecrypter {
    override suspend fun decrypt(event: EncryptedMessageContent) = throw IllegalStateException("No encrypter instance set")
}
