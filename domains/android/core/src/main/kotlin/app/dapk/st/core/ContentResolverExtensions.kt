package app.dapk.st.core

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

data class ContentResolverQuery(
    val uri: Uri,
    val projection: List<String>,
    val selection: String,
    val selectionArgs: List<String>,
    val sortBy: String,
)

inline fun <T> ContentResolver.reduce(query: ContentResolverQuery, operation: (Cursor) -> T): List<T> {
    val result = mutableListOf<T>()
    this.query(query.uri, query.projection.toTypedArray(), query.selection, query.selectionArgs.toTypedArray(), query.sortBy).use { cursor ->
        while (cursor != null && cursor.moveToNext()) {
            result.add(operation(cursor))
        }
    }
    return result
}
