package fake

import io.mockk.mockk
import java.io.InputStream

class FakeInputStream {
    val instance = mockk<InputStream>()
}
