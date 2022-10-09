package app.dapk.st.messenger

import android.content.Context
import android.os.Environment
import app.dapk.st.engine.MediaDecrypter
import app.dapk.st.engine.RoomEvent
import app.dapk.st.matrix.common.RoomId
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
import okio.BufferedSource
import okio.Path.Companion.toOkioPath
import java.io.File

class DecryptingFetcherFactory(
    private val context: Context,
    private val roomId: RoomId,
    private val mediaDecrypter: MediaDecrypter,
) : Fetcher.Factory<RoomEvent.Image> {

    override fun create(data: RoomEvent.Image, options: Options, imageLoader: ImageLoader): Fetcher {
        return DecryptingFetcher(data, context, mediaDecrypter, roomId)
    }
}

private val http = OkHttpClient()

class DecryptingFetcher(
    private val data: RoomEvent.Image,
    private val context: Context,
    private val mediaDecrypter: MediaDecrypter,
    roomId: RoomId,
) : Fetcher {

    private val directory by lazy {
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.resolve("SmallTalk/${roomId.value}").also { it.mkdirs() }
    }

    override suspend fun fetch(): FetchResult {
        val diskCacheKey = data.imageMeta.url.hashCode().toString()
        val diskCachedFile = directory.resolve(diskCacheKey)
        val path = diskCachedFile.toOkioPath()

        return when {
            diskCachedFile.exists() -> SourceResult(ImageSource(path), null, DataSource.DISK)

            else -> {
                diskCachedFile.createNewFile()
                val response = http.newCall(Request.Builder().url(data.imageMeta.url).build()).execute()
                when {
                    data.imageMeta.keys != null -> response.writeDecrypted(diskCachedFile, data.imageMeta.keys!!)
                    else -> response.body?.source()?.writeToFile(diskCachedFile) ?: throw IllegalArgumentException("No bitmap response found")
                }

                SourceResult(ImageSource(path), null, DataSource.NETWORK)
            }
        }
    }

    private fun Response.writeDecrypted(file: File, keys: RoomEvent.Image.ImageMeta.Keys) {
        this.body?.byteStream()?.let { byteStream ->
            file.outputStream().use { output ->
                mediaDecrypter.decrypt(byteStream, keys.k, keys.iv).collect { output.write(it) }
            }
        }
    }
}

private fun BufferedSource.writeToFile(file: File) {
    this.inputStream().use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

