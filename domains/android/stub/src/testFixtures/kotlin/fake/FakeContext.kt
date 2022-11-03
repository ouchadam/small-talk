package fake

import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import test.delegateReturn

class FakeContext {
    val instance = mockk<Context>()
    fun givenPackageManager() = every { instance.packageManager }.delegateReturn()
}

class FakePackageManager {
    val instance = mockk<PackageManager>()
}