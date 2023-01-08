package app.dapk.st.home

import app.dapk.st.core.BuildMeta
import app.dapk.st.domain.ApplicationPreferences
import app.dapk.st.domain.ApplicationVersion
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn
import test.expect

class BetaVersionUpgradeUseCaseTest {

    private val buildMeta = BuildMeta(versionName = "a-version-name", versionCode = 100, isDebug = false)
    private val fakeApplicationPreferences = FakeApplicationPreferences()

    private val useCase = BetaVersionUpgradeUseCase(
        fakeApplicationPreferences.instance,
        buildMeta
    )

    @Test
    fun `given same stored version, when hasVersionChanged then is false`() = runTest {
        fakeApplicationPreferences.givenVersion().returns(ApplicationVersion(buildMeta.versionCode))

        val result = useCase.hasVersionChanged()

        result shouldBeEqualTo false
    }

    // Should be impossible
    @Test
    fun `given higher stored version, when hasVersionChanged then is false`() = runTest {
        fakeApplicationPreferences.givenVersion().returns(ApplicationVersion(buildMeta.versionCode + 1))

        val result = useCase.hasVersionChanged()

        result shouldBeEqualTo false
    }

    @Test
    fun `given lower stored version, when hasVersionChanged then is true`() = runTest {
        fakeApplicationPreferences.givenVersion().returns(ApplicationVersion(buildMeta.versionCode - 1))

        val result = useCase.hasVersionChanged()

        result shouldBeEqualTo true
    }

    @Test
    fun `given version has changed, when waiting, then blocks until notified of upgrade`() = runTest {
        fakeApplicationPreferences.givenVersion().returns(ApplicationVersion(buildMeta.versionCode - 1))
        fakeApplicationPreferences.instance.expect { it.setVersion(ApplicationVersion(buildMeta.versionCode)) }

        val waitUntilReady = async { useCase.waitUnitReady() }
        async { useCase.notifyUpgraded() }
        waitUntilReady.await()
    }
}

private class FakeApplicationPreferences {
    val instance = mockk<ApplicationPreferences>()

    fun givenVersion() = coEvery { instance.readVersion() }.delegateReturn()
}