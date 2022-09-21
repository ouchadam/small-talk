package app.dapk.st.matrix.crypto

import app.dapk.st.core.Base64
import java.io.InputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val CRYPTO_BUFFER_SIZE = 32 * 1024
private const val CIPHER_ALGORITHM = "AES/CTR/NoPadding"
private const val SECRET_KEY_SPEC_ALGORITHM = "AES"
private const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"

class MediaDecrypter(private val base64: Base64) {

    fun decrypt(input: InputStream, k: String, iv: String): Collector {
        val key = base64.decode(k.replace('-', '+').replace('_', '/'))
        val initVectorBytes = base64.decode(iv)

        val decryptCipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val secretKeySpec = SecretKeySpec(key, SECRET_KEY_SPEC_ALGORITHM)
        val ivParameterSpec = IvParameterSpec(initVectorBytes)
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

        val messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)

        var read: Int
        val d = ByteArray(CRYPTO_BUFFER_SIZE)
        var decodedBytes: ByteArray

        return Collector { partial ->
            input.use {
                read = it.read(d)
                while (read != -1) {
                    messageDigest.update(d, 0, read)
                    decodedBytes = decryptCipher.update(d, 0, read)
                    partial(decodedBytes)
                    read = it.read(d)
                }
            }
        }
    }

}


fun interface Collector {
    fun collect(partial: (ByteArray) -> Unit)
}
