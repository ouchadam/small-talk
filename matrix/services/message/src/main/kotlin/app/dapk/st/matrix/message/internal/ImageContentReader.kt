package app.dapk.st.matrix.message.internal

import java.io.InputStream

interface ImageContentReader {
    fun meta(uri: String): ImageContent
    fun inputStream(uri: String): InputStream

    data class ImageContent(
        val height: Int,
        val width: Int,
        val size: Long,
        val fileName: String,
        val mimeType: String,
    )
}