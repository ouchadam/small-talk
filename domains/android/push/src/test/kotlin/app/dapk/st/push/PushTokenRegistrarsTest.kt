package app.dapk.st.push

import app.dapk.st.domain.push.PushTokenRegistrarPreferences
import app.dapk.st.push.messaging.MessagingPushTokenRegistrar
import app.dapk.st.push.unifiedpush.UnifiedPushRegistrar
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn
import test.runExpectTest

private val UNIFIED_PUSH = Registrar("unified-push option")
private val NONE = Registrar("None")
private val FIREBASE = Registrar("Google - Firebase (FCM)")
private val UNIFIED_PUSH_DISTRIBUTORS = listOf(UNIFIED_PUSH)

class PushTokenRegistrarsTest {

    private val fakeMessagingPushRegistrar = FakeMessagingPushRegistrar()
    private val fakeUnifiedPushRegistrar = FakeUnifiedPushRegistrar()
    private val fakePushTokenRegistrarPreferences = FakePushTokenRegistrarPreferences()
    private val selectionState = SelectionState(selection = null)

    private val registrars = PushTokenRegistrars(
        fakeMessagingPushRegistrar.instance,
        fakeUnifiedPushRegistrar.instance,
        fakePushTokenRegistrarPreferences.instance,
        selectionState,
    )

    @Test
    fun `given messaging is available, when reading options, then returns firebase and unified push`() {
        fakeMessagingPushRegistrar.givenIsAvailable().returns(true)
        fakeUnifiedPushRegistrar.givenDistributors().returns(UNIFIED_PUSH_DISTRIBUTORS)

        val result = registrars.options()

        result shouldBeEqualTo listOf(Registrar("None"), FIREBASE) + UNIFIED_PUSH_DISTRIBUTORS
    }

    @Test
    fun `given messaging is not available, when reading options, then returns unified push`() {
        fakeMessagingPushRegistrar.givenIsAvailable().returns(false)
        fakeUnifiedPushRegistrar.givenDistributors().returns(UNIFIED_PUSH_DISTRIBUTORS)

        val result = registrars.options()

        result shouldBeEqualTo listOf(Registrar("None")) + UNIFIED_PUSH_DISTRIBUTORS
    }

    @Test
    fun `given no saved selection and messaging is not available, when reading default selection, then returns none`() = runTest {
        fakePushTokenRegistrarPreferences.givenCurrentSelection().returns(null)
        fakeMessagingPushRegistrar.givenIsAvailable().returns(false)

        val result = registrars.currentSelection()

        result shouldBeEqualTo NONE
    }

    @Test
    fun `given no saved selection and messaging is available, when reading default selection, then returns firebase`() = runTest {
        fakePushTokenRegistrarPreferences.givenCurrentSelection().returns(null)
        fakeMessagingPushRegistrar.givenIsAvailable().returns(true)

        val result = registrars.currentSelection()

        result shouldBeEqualTo FIREBASE
    }

    @Test
    fun `given saved selection and is a option, when reading default selection, then returns selection`() = runTest {
        fakeUnifiedPushRegistrar.givenDistributors().returns(UNIFIED_PUSH_DISTRIBUTORS)
        fakePushTokenRegistrarPreferences.givenCurrentSelection().returns(FIREBASE.id)
        fakeMessagingPushRegistrar.givenIsAvailable().returns(true)

        val result = registrars.currentSelection()

        result shouldBeEqualTo FIREBASE
    }

    @Test
    fun `given saved selection and is not an option, when reading default selection, then returns next default`() = runTest {
        fakeUnifiedPushRegistrar.givenDistributors().returns(UNIFIED_PUSH_DISTRIBUTORS)
        fakePushTokenRegistrarPreferences.givenCurrentSelection().returns(FIREBASE.id)
        fakeMessagingPushRegistrar.givenIsAvailable().returns(false)

        val result = registrars.currentSelection()

        result shouldBeEqualTo NONE
    }

    @Test
    fun `when selecting none, then stores and unregisters`() = runExpectTest {
        fakePushTokenRegistrarPreferences.instance.expect { it.store(NONE.id) }
        fakeMessagingPushRegistrar.instance.expect { it.unregister() }
        fakeUnifiedPushRegistrar.instance.expect { it.unregister() }

        registrars.makeSelection(NONE)

        verifyExpects()
    }

    @Test
    fun `when selecting firebase, then stores and unregisters unifiedpush`() = runExpectTest {
        fakePushTokenRegistrarPreferences.instance.expect { it.store(FIREBASE.id) }
        fakeMessagingPushRegistrar.instance.expect { it.registerCurrentToken() }
        fakeUnifiedPushRegistrar.instance.expect { it.unregister() }

        registrars.makeSelection(FIREBASE)

        verifyExpects()
    }

    @Test
    fun `when selecting unified push, then stores and unregisters firebase`() = runExpectTest {
        fakePushTokenRegistrarPreferences.instance.expect { it.store(UNIFIED_PUSH.id) }
        fakeMessagingPushRegistrar.instance.expect { it.unregister() }
        fakeUnifiedPushRegistrar.instance.expect { it.registerSelection(UNIFIED_PUSH) }

        registrars.makeSelection(UNIFIED_PUSH)

        verifyExpects()
    }

    @Test
    fun `given unified push selected, when registering current token, then delegates`() = runExpectTest {
        selectionState.selection = UNIFIED_PUSH
        fakeUnifiedPushRegistrar.instance.expect { it.registerCurrentToken() }

        registrars.registerCurrentToken()

        verifyExpects()
    }

    @Test
    fun `given firebase selected, when registering current token, then delegates`() = runExpectTest {
        selectionState.selection = FIREBASE
        fakeMessagingPushRegistrar.instance.expect { it.registerCurrentToken() }

        registrars.registerCurrentToken()

        verifyExpects()
    }

    @Test
    fun `given none selected, when registering current token, then does nothing`() = runExpectTest {
        selectionState.selection = NONE

        registrars.registerCurrentToken()

        verify { fakeMessagingPushRegistrar.instance wasNot Called }
        verify { fakeUnifiedPushRegistrar.instance wasNot Called }
    }

    @Test
    fun `given unified push selected, when unregistering, then delegates`() = runExpectTest {
        selectionState.selection = UNIFIED_PUSH
        fakeUnifiedPushRegistrar.instance.expect { it.unregister() }

        registrars.unregister()

        verifyExpects()
    }

    @Test
    fun `given firebase selected, when unregistering, then delegates`() = runExpectTest {
        selectionState.selection = FIREBASE
        fakeMessagingPushRegistrar.instance.expect { it.unregister() }

        registrars.unregister()

        verifyExpects()
    }

    @Test
    fun `given none selected, when unregistering, then unregisters all`() = runExpectTest {
        selectionState.selection = NONE
        fakeUnifiedPushRegistrar.instance.expect { it.unregister() }
        fakeMessagingPushRegistrar.instance.expect { it.unregister() }

        registrars.unregister()

        verifyExpects()
    }
}

class FakeMessagingPushRegistrar {
    val instance = mockk<MessagingPushTokenRegistrar>()

    fun givenIsAvailable() = every { instance.isAvailable() }.delegateReturn()
}

class FakeUnifiedPushRegistrar {
    val instance = mockk<UnifiedPushRegistrar>()

    fun givenDistributors() = every { instance.getDistributors() }.delegateReturn()
}

class FakePushTokenRegistrarPreferences {
    val instance = mockk<PushTokenRegistrarPreferences>()

    fun givenCurrentSelection() = coEvery { instance.currentSelection() }.delegateReturn()
}