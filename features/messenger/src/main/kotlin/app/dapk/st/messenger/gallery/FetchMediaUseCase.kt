package app.dapk.st.messenger.gallery

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import app.dapk.st.core.ContentResolverQuery
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.reduce
import app.dapk.st.core.withIoContext

class FetchMediaUseCase(
    private val contentResolver: ContentResolver,
    private val uriAvoidance: MediaUriAvoidance,
    private val dispatchers: CoroutineDispatchers
) {

    private val projection = listOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.ORIENTATION,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.SIZE
    )

    private val selection = MediaStore.Images.Media.BUCKET_ID + " = ? AND " + isNotPending() + " AND " + MediaStore.Images.Media.MIME_TYPE + " NOT LIKE ?"

    suspend fun getMediaInBucket(bucketId: String): List<Media> {
        return dispatchers.withIoContext {
            val query = ContentResolverQuery(
                uri = uriAvoidance.externalContentUri,
                projection = projection,
                selection = selection,
                selectionArgs = listOf(bucketId, "%image/svg%"),
                sortBy = MediaStore.Images.Media.DATE_MODIFIED + " DESC",
            )

            contentResolver.reduce(query) { cursor ->
                val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val uri = uriAvoidance.uriAppender(query.uri, rowId)
                val mimetype = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                val orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION))
                val width = cursor.getInt(cursor.getColumnIndexOrThrow(getWidthColumn(orientation)))
                val height = cursor.getInt(cursor.getColumnIndexOrThrow(getHeightColumn(orientation)))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                Media(rowId, uri, mimetype, width, height, size, date)
            }
        }
    }

    private fun getWidthColumn(orientation: Int) = if (orientation == 0 || orientation == 180) MediaStore.Images.Media.WIDTH else MediaStore.Images.Media.HEIGHT

    private fun getHeightColumn(orientation: Int) =
        if (orientation == 0 || orientation == 180) MediaStore.Images.Media.HEIGHT else MediaStore.Images.Media.WIDTH
}

data class Media(
    val id: Long,
    val uri: Uri,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val dateModifiedEpochMillis: Long,
)

