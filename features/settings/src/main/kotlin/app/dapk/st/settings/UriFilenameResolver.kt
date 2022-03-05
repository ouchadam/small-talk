package app.dapk.st.settings

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext

class UriFilenameResolver(
    private val contentResolver: ContentResolver,
    private val coroutineDispatchers: CoroutineDispatchers
) {

    suspend fun readFilenameFromUri(uri: Uri): String {
        val fallback = uri.path?.substringAfterLast('/') ?: throw IllegalStateException("expecting a file uri but got $uri")
        return when (uri.scheme) {
            "content" -> readResolvedDisplayName(uri) ?: fallback
            else -> fallback
        }
    }

    private suspend fun readResolvedDisplayName(uri: Uri): String? {
        return coroutineDispatchers.withIoContext {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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
}