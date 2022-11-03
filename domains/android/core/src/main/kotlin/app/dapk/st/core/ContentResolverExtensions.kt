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
    return this.reduce(query, mutableListOf<T>()) { acc, cursor ->
        acc.add(operation(cursor))
        acc
    }
}

inline fun <T> ContentResolver.reduce(query: ContentResolverQuery, initial: T, operation: (T, Cursor) -> T): T {
    var accumulator: T = initial
    this.query(query.uri, query.projection.toTypedArray(), query.selection, query.selectionArgs.toTypedArray(), query.sortBy).use { cursor ->
        while (cursor != null && cursor.moveToNext()) {
            accumulator = operation(accumulator, cursor)
        }
    }
    return accumulator
}
