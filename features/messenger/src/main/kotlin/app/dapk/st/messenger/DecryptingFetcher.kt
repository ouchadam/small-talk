package app.dapk.st.messenger

import android.content.Context
import app.dapk.st.core.Base64
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

class DecryptingFetcherFactory(private val context: Context, base64: Base64) : Fetcher.Factory<RoomEvent.Image> {

    private val mediaDecrypter = MediaDecrypter(base64)

    override fun create(data: RoomEvent.Image, options: Options, imageLoader: ImageLoader): Fetcher {
        return DecryptingFetcher(data, context, mediaDecrypter)
    }
}

private val http = OkHttpClient()

class DecryptingFetcher(
    private val data: RoomEvent.Image,
    private val context: Context,
    private val mediaDecrypter: MediaDecrypter,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val response = http.newCall(Request.Builder().url(data.imageMeta.url).build()).execute()
        val outputStream = when {
            data.imageMeta.keys != null -> handleEncrypted(response, data.imageMeta.keys!!)
            else -> response.body?.source() ?: throw IllegalArgumentException("No bitmap response found")
        }
        return SourceResult(ImageSource(outputStream, context), null, DataSource.NETWORK)
    }

    private fun handleEncrypted(response: Response, keys: RoomEvent.Image.ImageMeta.Keys): Buffer {
        return response.body?.byteStream()?.let { mediaDecrypter.decrypt(it, keys.k, keys.iv) } ?: Buffer()
    }
}

