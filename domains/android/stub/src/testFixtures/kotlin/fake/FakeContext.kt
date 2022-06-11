package fake

import android.content.Context
import io.mockk.mockk

class FakeContext {
    val instance = mockk<Context>()
}