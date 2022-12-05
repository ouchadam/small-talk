package app.dapk.st.impl

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import app.dapk.st.engine.ImageContentReader
import java.io.InputStream

internal class AndroidImageContentReader(private val contentResolver: ContentResolver) : ImageContentReader {
    override fun meta(uri: String): ImageContentReader.ImageContent {
        val androidUri = Uri.parse(uri)
        val fileStream = contentResolver.openInputStream(androidUri) ?: throw IllegalArgumentException("Could not process $uri")

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(fileStream, null, options)

        val fileSize = contentResolver.query(androidUri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.getLong(columnIndex)
        } ?: throw IllegalArgumentException("Could not process $uri")

        val shouldSwapSizes = ExifInterface(contentResolver.openInputStream(androidUri) ?: throw IllegalArgumentException("Could not process $uri")).let {
            val orientation = it.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270
        }

        return ImageContentReader.ImageContent(
            height = if (shouldSwapSizes) options.outWidth else options.outHeight,
            width = if (shouldSwapSizes) options.outHeight else options.outWidth,
            size = fileSize,
            mimeType = options.outMimeType,
            fileName = androidUri.lastPathSegment ?: "file",
        )
    }

    override fun inputStream(uri: String): InputStream = contentResolver.openInputStream(Uri.parse(uri))!!
}