package app.dapk.st.messenger

import android.content.Context
import android.util.Base64
import app.dapk.st.matrix.sync.RoomEvent
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val CRYPTO_BUFFER_SIZE = 32 * 1024
private const val CIPHER_ALGORITHM = "AES/CTR/NoPadding"
private const val SECRET_KEY_SPEC_ALGORITHM = "AES"
private const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"

class DecryptingFetcherFactory(private val context: Context) : Fetcher.Factory<RoomEvent.Image> {
    override fun create(data: RoomEvent.Image, options: Options, imageLoader: ImageLoader): Fetcher {
        return DecryptingFetcher(data, context)
    }
}

private val http = OkHttpClient()

class DecryptingFetcher(private val data: RoomEvent.Image, private val context: Context) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val response = http.newCall(Request.Builder().url(data.imageMeta.url).build()).execute()
        val outputStream = when {
            data.imageMeta.keys != null -> handleEncrypted(response, data.imageMeta.keys!!)
            else -> response.body?.source() ?: throw IllegalArgumentException("No bitmap response found")
        }
        return SourceResult(ImageSource(outputStream, context), null, DataSource.NETWORK)
    }

    private fun handleEncrypted(response: Response, keys: RoomEvent.Image.ImageMeta.Keys): Buffer {
        val key = Base64.decode(keys.k.replace('-', '+').replace('_', '/'), Base64.DEFAULT)
        val initVectorBytes = Base64.decode(keys.iv, Base64.DEFAULT)

        val decryptCipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val secretKeySpec = SecretKeySpec(key, SECRET_KEY_SPEC_ALGORITHM)
        val ivParameterSpec = IvParameterSpec(initVectorBytes)
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

        val messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)

        var read: Int
        val d = ByteArray(CRYPTO_BUFFER_SIZE)
        var decodedBytes: ByteArray

        val outputStream = Buffer()
        response.body?.let {
            it.byteStream().use {
                read = it.read(d)
                while (read != -1) {
                    messageDigest.update(d, 0, read)
                    decodedBytes = decryptCipher.update(d, 0, read)
                    outputStream.write(decodedBytes)
                    read = it.read(d)
                }
            }
        }
        return outputStream
    }
}