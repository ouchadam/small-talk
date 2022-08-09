package app.dapk.st.matrix.message.internal

interface ImageContentReader {
    fun read(uri: String): ImageContent

    data class ImageContent(
        val height: Int,
        val width: Int,
        val size: Long,
        val fileName: String,
        val mimeType: String,
        val content: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ImageContent

            if (height != other.height) return false
            if (width != other.width) return false
            if (size != other.size) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = height
            result = 31 * result + width
            result = 31 * result + size.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }
}