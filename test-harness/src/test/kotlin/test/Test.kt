@file:OptIn(ExperimentalCoroutinesApi::class)

package test

import TestMessage
import TestUser
import app.dapk.st.core.extensions.ifNull
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.message.messageService
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.syncService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import java.util.*

fun flowTest(block: suspend MatrixTestScope.() -> Unit) {
    runTest {
        val testScope = MatrixTestScope(this)
        block(testScope)
    }
}

fun restoreLoginAndInitialSync(m1: TestMatrix, m2: TestMatrix, testBody: suspend MatrixTestScope.(TestMatrix, TestMatrix) -> Unit) {
    runTest {
        println("restore login 1")
        m1.restoreLogin()
        println("restore login 2")
        m2.restoreLogin()
        val testHelper = MatrixTestScope(this)
        testHelper.testMatrix(m1)
        testHelper.testMatrix(m2)
        with(testHelper) {
            combine(m1.client.syncService().startSyncing(), m2.client.syncService().startSyncing()) { _, _ -> }.collectAsync {
                m1.client.syncService().overview().first()
                m2.client.syncService().overview().first()
                testBody(testHelper, m1, m2)
            }
        }
        testHelper.release()
    }
}

suspend fun <T> Flow<T>.collectAsync(scope: CoroutineScope, block: suspend () -> Unit) {
    val work = scope.async {
        withContext(Dispatchers.IO) {
            collect()
        }
    }
    block()
    work.cancelAndJoin()
}

class MatrixTestScope(private val testScope: TestScope) {

    private val inProgressExpects = mutableListOf<Deferred<*>>()
    private val inProgressInstances = mutableListOf<TestMatrix>()

    suspend fun <T> Flow<T>.collectAsync(block: suspend () -> Unit) {
        collectAsync(testScope, block)
    }

    suspend fun delay(amountMs: Long) {
        withContext(Dispatchers.Unconfined) {
            kotlinx.coroutines.delay(amountMs)
        }
    }

    suspend fun <T> Flow<T>.expectAsync(matcher: (T) -> Boolean) {
        val flow = this

        inProgressExpects.add(testScope.async(Dispatchers.Unconfined) {
            flow.first { matcher(it) }
        })
    }

    suspend fun waitForExpects() {
        inProgressExpects.awaitAll()
    }

    suspend fun <T> Flow<T>.expect(matcher: (T) -> Boolean) {
        val flow = this

        val collected = mutableListOf<T>()
        val work = testScope.async {
            flow.onEach {
                collected.add(it)
            }.first { matcher(it) }
        }
        withContext(Dispatchers.Unconfined) {
            withTimeoutOrNull(5000) { work.await() }
        }.ifNull {
            fail("found no matches in $collected")
        }
    }

    suspend fun <T> Flow<T>.assert(expected: T) {
        val flow = this

        val collected = mutableListOf<T>()
        val work = testScope.async {
            flow.onEach {
                println("found: $it")
                collected.add(it)
            }.first { it == expected }
        }
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(5000) { work.await() }
        }
        collected.lastOrNull() shouldBeEqualTo expected
    }

    suspend fun TestMatrix.expectRoom(roomId: RoomId) {
        this.client.syncService().overview()
            .expect { it.any { it.roomId == roomId } }
    }

    suspend fun TestMatrix.expectInvite(roomId: RoomId) {
        this.client.syncService().invites()
            .expect { it.any { it.roomId == roomId } }
    }

    suspend fun TestMatrix.expectTextMessage(roomId: RoomId, message: TestMessage) {
        println("expecting ${message.content}")
        this.client.syncService().room(roomId)
            .map { it.events.filterIsInstance<RoomEvent.Message>().map { TestMessage(it.content, it.author) }.firstOrNull() }
            .assert(message)
    }

    suspend fun TestMatrix.sendTextMessage(roomId: RoomId, content: String, isEncrypted: Boolean) {
        println("sending $content")
        this.client.messageService().scheduleMessage(
            MessageService.Message.TextMessage(
                content = MessageService.Message.Content.TextContent(body = content),
                roomId = roomId,
                sendEncrypted = isEncrypted,
                localId = "local.${UUID.randomUUID()}",
                timestampUtc = System.currentTimeMillis(),
            )
        )
    }

    suspend fun TestMatrix.loginWithInitialSync() {
        this.restoreLogin()
        client.syncService().startSyncing().collectAsync(testScope) {
            client.syncService().overview().first()
        }
    }

    fun testMatrix(user: TestUser, isTemp: Boolean, withLogging: Boolean = false) = TestMatrix(
        user,
        temporaryDatabase = isTemp,
        includeLogging = withLogging
    ).also { inProgressInstances.add(it) }

    fun testMatrix(testMatrix: TestMatrix) = inProgressInstances.add(testMatrix)

    suspend fun release() {
        inProgressInstances.forEach { it.release() }
    }

}