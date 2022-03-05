package app.dapk.st.settings

import android.provider.OpenableColumns
import app.dapk.st.core.CoroutineDispatchers
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import kotlin.test.assertFailsWith

private const val A_LAST_SEGMENT = "a-file-name.foo"
private const val A_DISPLAY_NAME = "file-display-name.foo"

class UriFilenameResolverTest {

    private val fakeUri = FakeUri()
    private val fakeContentResolver = FakeContentResolver()
    private val uriFilenameResolver = UriFilenameResolver(fakeContentResolver.instance, CoroutineDispatchers())

    @Test
    fun `given a non hierarchical Uri when querying file name then throws`() = runTest {
        assertFailsWith<IllegalStateException> {
            fakeUri.givenNonHierarchical()

            uriFilenameResolver.readFilenameFromUri(fakeUri.instance)
        }
    }

    @Test
    fun `given a non content schema Uri when querying file name then returns last segment`() = runTest {
        fakeUri.givenContent(schema = "file", path = "path/to/$A_LAST_SEGMENT")

        val result = uriFilenameResolver.readFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo A_LAST_SEGMENT
    }

    @Test
    fun `given content schema Uri with no backing content when querying file name then returns last segment`() = runTest {
        fakeUri.givenContent(schema = "content", path = "path/to/$A_LAST_SEGMENT")
        fakeContentResolver.givenUriResult(fakeUri.instance).returns(null)

        val result = uriFilenameResolver.readFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo A_LAST_SEGMENT
    }

    @Test
    fun `given content schema Uri with empty backing content when querying file name then returns last segment`() = runTest {
        fakeUri.givenContent(schema = "content", path = "path/to/$A_LAST_SEGMENT")
        val emptyCursor = FakeCursor().also { it.givenEmpty() }
        fakeContentResolver.givenUriResult(fakeUri.instance).returns(emptyCursor.instance)

        val result = uriFilenameResolver.readFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo A_LAST_SEGMENT
    }

    @Test
    fun `given content schema Uri with backing content when querying file name then returns display name column`() = runTest {
        fakeUri.givenContent(schema = "content", path = "path/to/$A_DISPLAY_NAME")
        val aCursor = FakeCursor().also { it.givenString(OpenableColumns.DISPLAY_NAME, A_DISPLAY_NAME) }
        fakeContentResolver.givenUriResult(fakeUri.instance).returns(aCursor.instance)

        val result = uriFilenameResolver.readFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo A_DISPLAY_NAME
    }
}
