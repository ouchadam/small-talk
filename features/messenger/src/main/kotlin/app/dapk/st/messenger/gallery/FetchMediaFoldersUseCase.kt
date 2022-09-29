package app.dapk.st.messenger.gallery

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// https://github.com/signalapp/Signal-Android/blob/e22ddb8f96f8801f0abe622b5261abc6cb396d94/app/src/main/java/org/thoughtcrime/securesms/mediasend/MediaRepository.java

class FetchMediaFoldersUseCase(
    private val contentResolver: ContentResolver,
) {

    suspend fun fetchFolders(): List<Folder> {
        return withContext(Dispatchers.IO) {
            val projection = arrayOf(Images.Media._ID, Images.Media.BUCKET_ID, Images.Media.BUCKET_DISPLAY_NAME, Images.Media.DATE_MODIFIED)
            val selection = "${isNotPending()} AND ${Images.Media.BUCKET_ID} AND ${Images.Media.MIME_TYPE} NOT LIKE ?"
            val sortBy = "${Images.Media.BUCKET_DISPLAY_NAME} COLLATE NOCASE ASC, ${Images.Media.DATE_MODIFIED} DESC"

            val folders = mutableMapOf<String, Folder>()
            val contentUri = Images.Media.EXTERNAL_CONTENT_URI
            contentResolver.query(contentUri, projection, selection, arrayOf("%image/svg%"), sortBy).use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]))
                    val thumbnail = ContentUris.withAppendedId(contentUri, rowId)
                    val bucketId = cursor.getString(cursor.getColumnIndexOrThrow(projection[1]))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(projection[2])) ?: ""
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(projection[3]))
                    val folder = folders.getOrPut(bucketId) { Folder(bucketId, title, thumbnail) }
                    folder.incrementItemCount()
                }
            }
            folders.values.toList()
        }
    }


}

data class Folder(
    val bucketId: String,
    val title: String,
    val thumbnail: Uri,
) {
    private var _itemCount: Long = 0L
    val itemCount: Long
        get() = _itemCount

    fun incrementItemCount() {
        _itemCount++
    }

}


class FetchMediaUseCase(private val contentResolver: ContentResolver) {

    private val projection = arrayOf(
        Images.Media._ID,
        Images.Media.MIME_TYPE,
        Images.Media.DATE_MODIFIED,
        Images.Media.ORIENTATION,
        Images.Media.WIDTH,
        Images.Media.HEIGHT,
        Images.Media.SIZE
    )

    suspend fun getMediaInBucket(bucketId: String): List<Media> {
        return withContext(Dispatchers.IO) {

            val media = mutableListOf<Media>()
            val selection = Images.Media.BUCKET_ID + " = ? AND " + isNotPending() + " AND " + Images.Media.MIME_TYPE + " NOT LIKE ?"
            val selectionArgs = arrayOf(bucketId, "%image/svg%")
            val sortBy = Images.Media.DATE_MODIFIED + " DESC"
            val contentUri = Images.Media.EXTERNAL_CONTENT_URI
            contentResolver.query(contentUri, projection, selection, selectionArgs, sortBy).use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]))
                    val uri = ContentUris.withAppendedId(contentUri, rowId)
                    val mimetype = cursor.getString(cursor.getColumnIndexOrThrow(Images.Media.MIME_TYPE))
                    val date = cursor.getLong(cursor.getColumnIndexOrThrow(Images.Media.DATE_MODIFIED))
                    val orientation = cursor.getInt(cursor.getColumnIndexOrThrow(Images.Media.ORIENTATION))
                    val width = cursor.getInt(cursor.getColumnIndexOrThrow(getWidthColumn(orientation)))
                    val height = cursor.getInt(cursor.getColumnIndexOrThrow(getHeightColumn(orientation)))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(Images.Media.SIZE))
                    media.add(Media(rowId, uri, mimetype, width, height, size, date))
                }
            }
            media
        }
    }

    private fun getWidthColumn(orientation: Int) = if (orientation == 0 || orientation == 180) Images.Media.WIDTH else Images.Media.HEIGHT

    private fun getHeightColumn(orientation: Int) = if (orientation == 0 || orientation == 180) Images.Media.HEIGHT else Images.Media.WIDTH

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

private fun isNotPending() = if (Build.VERSION.SDK_INT <= 28) Images.Media.DATA + " NOT NULL" else MediaStore.MediaColumns.IS_PENDING + " != 1"
