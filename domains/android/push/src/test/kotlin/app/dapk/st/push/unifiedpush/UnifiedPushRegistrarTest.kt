package app.dapk.st.push.unifiedpush

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.dapk.st.push.Registrar
import fake.FakeContext
import fake.FakePackageManager
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import test.delegateReturn
import test.runExpectTest

private val A_COMPONENT_NAME = FakeComponentName()
private val A_REGISTRAR_SELECTION = Registrar("a-registrar")
private const val A_SAVED_DISTRIBUTOR = "a distributor"

class UnifiedPushRegistrarTest {

    private val fakePackageManager = FakePackageManager()
    private val fakeContext = FakeContext().also {
        it.givenPackageManager().returns(fakePackageManager.instance)
    }
    private val fakeUnifiedPush = FakeUnifiedPush()
    private val fakeComponentFactory = { _: Context -> A_COMPONENT_NAME.instance }

    private val registrar = UnifiedPushRegistrar(fakeContext.instance, fakeUnifiedPush, fakeComponentFactory)

    @Test
    fun `when unregistering, then updates unified push and disables component`() = runExpectTest {
        fakeUnifiedPush.expect { it.unregisterApp(fakeContext.instance) }
        fakePackageManager.instance.expect {
            it.setComponentEnabledSetting(A_COMPONENT_NAME.instance, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }

        registrar.unregister()

        verifyExpects()
    }

    @Test
    fun `when registering selection, then updates unified push and enables component`() = runExpectTest {
        fakeUnifiedPush.expect { it.registerApp(fakeContext.instance) }
        fakeUnifiedPush.expect { it.saveDistributor(fakeContext.instance, A_REGISTRAR_SELECTION.id) }
        fakePackageManager.instance.expect {
            it.setComponentEnabledSetting(A_COMPONENT_NAME.instance, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }

        registrar.registerSelection(A_REGISTRAR_SELECTION)

        verifyExpects()
    }

    @Test
    fun `given saved distributor, when registering current token, then updates unified push and enables component`() = runExpectTest {
        fakeUnifiedPush.givenDistributor(fakeContext.instance).returns(A_SAVED_DISTRIBUTOR)
        fakeUnifiedPush.expect { it.registerApp(fakeContext.instance) }
        fakePackageManager.instance.expect {
            it.setComponentEnabledSetting(A_COMPONENT_NAME.instance, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }

        registrar.registerCurrentToken()

        verifyExpects()
    }

    @Test
    fun `given no distributor, when registering current token, then does nothing`() = runExpectTest {
        fakeUnifiedPush.givenDistributor(fakeContext.instance).returns("")

        registrar.registerCurrentToken()

        verify(exactly = 0) { fakeUnifiedPush.registerApp(any()) }
        verify { fakePackageManager.instance wasNot Called }
    }
}


class FakeUnifiedPush : UnifiedPush by mockk() {
    fun givenDistributor(context: Context) = every { getDistributor(context) }.delegateReturn()
}

class FakeComponentName {
    val instance = mockk<ComponentName>()
}