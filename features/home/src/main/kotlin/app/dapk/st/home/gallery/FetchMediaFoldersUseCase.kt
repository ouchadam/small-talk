package app.dapk.st.home.gallery

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
        val projection = arrayOf(Images.Media._ID, Images.Media.BUCKET_ID, Images.Media.BUCKET_DISPLAY_NAME, Images.Media.DATE_MODIFIED)
        val selection = "${isNotPending()} AND ${Images.Media.BUCKET_ID} AND ${Images.Media.MIME_TYPE} NOT LIKE ?"
        val sortBy = "${Images.Media.BUCKET_DISPLAY_NAME} COLLATE NOCASE ASC, ${Images.Media.DATE_MODIFIED} DESC"

        val folders = mutableMapOf<String, Folder>()
        val contentUri = Images.Media.EXTERNAL_CONTENT_URI
        withContext(Dispatchers.IO) {
            contentResolver.query(contentUri, projection, selection, arrayOf("%image/svg%"), sortBy).use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]))
                    val thumbnail = ContentUris.withAppendedId(contentUri, rowId)
                    val bucketId = cursor.getString(cursor.getColumnIndexOrThrow(projection[1]))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(projection[2])) ?: ""
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(projection[3]))

                    val folder = folders.getOrPut(bucketId) { Folder(bucketId, title, thumbnail) }
                    folder.incrementItemCount()

//                val folder: FolderData = Util.getOrDefault(folders, bucketId, FolderData(thumbnail, localizeTitle(context, title), bucketId))
//                folder.incrementCount()
//                folders.put(bucketId, folder)
//                if (cameraBucketId == null && title == "Camera") {
//                    cameraBucketId = bucketId
//                }
//                if (timestamp > thumbnailTimestamp) {
//                    globalThumbnail = thumbnail
//                    thumbnailTimestamp = timestamp
//                }
                }
            }
        }
        return folders.values.toList()
    }

    private fun isNotPending() = if (Build.VERSION.SDK_INT <= 28) Images.Media.DATA + " NOT NULL" else MediaStore.MediaColumns.IS_PENDING + " != 1"

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