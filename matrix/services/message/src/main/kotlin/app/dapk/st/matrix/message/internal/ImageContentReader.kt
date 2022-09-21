package app.dapk.st.matrix.message.internal

import java.io.File
import java.net.URI

interface ImageContentReader {
    fun read(uri: String): ImageContent

    data class ImageContent(
        val height: Int,
        val width: Int,
        val size: Long,
        val fileName: String,
        val mimeType: String,
        val uri: URI
    ) {
        fun inputStream() = File(uri).inputStream()
        fun outputStream() = File(uri).outputStream()
    }
}