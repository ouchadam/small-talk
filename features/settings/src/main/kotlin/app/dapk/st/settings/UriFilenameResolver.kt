package app.dapk.st.settings

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

class UriFilenameResolver(private val contentResolver: ContentResolver) {

    fun readFilenameFromUri(uri: Uri): String {
        val fallback = uri.path?.substringAfterLast('/') ?: throw IllegalStateException("expecting a file uri but got $uri")
        return when (uri.scheme) {
            "content" -> readResolvedDisplayName(uri) ?: fallback
            else -> fallback
        }
    }

    private fun readResolvedDisplayName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            when {
                cursor.moveToFirst() -> {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        .takeIf { it != -1 }
                        ?.let { cursor.getString(it) }
                }
                else -> null
            }
        }
    }
}