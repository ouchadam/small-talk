package app.dapk.st.messenger.gallery

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore.Images
import app.dapk.st.core.ContentResolverQuery
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.reduce
import app.dapk.st.core.withIoContext

class FetchMediaFoldersUseCase(
    private val contentResolver: ContentResolver,
    private val uriAvoidance: MediaUriAvoidance,
    private val dispatchers: CoroutineDispatchers,
) {

    suspend fun fetchFolders(): List<Folder> {
        return dispatchers.withIoContext {
            val query = ContentResolverQuery(
                uriAvoidance.externalContentUri,
                listOf(Images.Media._ID, Images.Media.BUCKET_ID, Images.Media.BUCKET_DISPLAY_NAME, Images.Media.DATE_MODIFIED),
                "${isNotPending()} AND ${Images.Media.BUCKET_ID} AND ${Images.Media.MIME_TYPE} NOT LIKE ?",
                listOf("%image/svg%"),
                "${Images.Media.BUCKET_DISPLAY_NAME} COLLATE NOCASE ASC, ${Images.Media.DATE_MODIFIED} DESC"
            )

            contentResolver.reduce(query, mutableMapOf<String, Folder>()) { acc, cursor ->
                val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(Images.Media._ID))
                val thumbnail = uriAvoidance.uriAppender(query.uri, rowId)
                val bucketId = cursor.getString(cursor.getColumnIndexOrThrow(Images.Media.BUCKET_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(Images.Media.BUCKET_DISPLAY_NAME)) ?: ""
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Images.Media.DATE_MODIFIED))
                val folder = acc.getOrPut(bucketId) { Folder(bucketId, title, thumbnail) }
                folder.incrementItemCount()
                acc
            }.values.toList()
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
