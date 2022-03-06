package fake

import android.content.ContentResolver
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import test.delegateReturn

class FakeContentResolver {

    val instance = mockk<ContentResolver>()

    fun givenFile(uri: Uri) = every { instance.openInputStream(uri) }.delegateReturn()

    fun givenUriResult(uri: Uri) = every { instance.query(uri, null, null, null, null) }.delegateReturn()
}
