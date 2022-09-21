package app.dapk.st.matrix.message

import java.io.File
import java.io.InputStream
import java.net.URI

fun interface MediaEncrypter {

    suspend fun encrypt(input: InputStream): Result

    data class Result(
        val uri: URI,
        val algorithm: String,
        val ext: Boolean,
        val keyOperations: List<String>,
        val kty: String,
        val k: String,
        val iv: String,
        val hashes: Map<String, String>,
        val v: String,
    ) {

        fun openStream() = File(uri).outputStream()
    }

}

internal object MissingMediaEncrypter : MediaEncrypter {
    override suspend fun encrypt(input: InputStream) = throw IllegalStateException("No encrypter instance set")
}
