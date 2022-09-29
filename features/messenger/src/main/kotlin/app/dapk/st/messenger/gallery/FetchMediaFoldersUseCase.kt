package app.dapk.st.messenger.gallery

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore.Images
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext

class FetchMediaFoldersUseCase(
    private val contentResolver: ContentResolver,
    private val dispatchers: CoroutineDispatchers,
) {

    suspend fun fetchFolders(): List<Folder> {
        return dispatchers.withIoContext {
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
