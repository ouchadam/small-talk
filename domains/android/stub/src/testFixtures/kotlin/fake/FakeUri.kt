package fake

import android.net.Uri
import io.mockk.every
import io.mockk.mockk

class FakeUri {
    val instance = mockk<Uri>()

    fun givenNonHierarchical() {
        givenContent(schema = "mail", path = null)
    }

    fun givenContent(schema: String, path: String?) {
        every { instance.scheme } returns schema
        every { instance.path } returns path
    }
}