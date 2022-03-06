package internalfake

import android.net.Uri
import app.dapk.st.settings.UriFilenameResolver
import io.mockk.coEvery
import io.mockk.mockk

class FakeUriFilenameResolver {
    val instance = mockk<UriFilenameResolver>()

    fun givenFilename(uri: Uri) = coEvery { instance.readFilenameFromUri(uri) }
}