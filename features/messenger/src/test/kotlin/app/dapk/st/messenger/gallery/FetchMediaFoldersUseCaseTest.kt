package app.dapk.st.messenger.gallery

import android.net.Uri
import android.provider.MediaStore
import fake.CreateCursorScope
import fake.FakeContentResolver
import fake.FakeUri
import fake.createCursor
import fixture.CoroutineDispatchersFixture.aCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_EXTERNAL_CONTENT_URI = FakeUri()
private const val A_BUCKET_ID = "a-bucket-id"
private const val A_SECOND_BUCKET_ID = "another-bucket"
private const val A_DISPLAY_NAME = "a-bucket-name"
private const val A_DATE_MODIFIED = 5000L

class FetchMediaFoldersUseCaseTest {

    private val fakeContentResolver = FakeContentResolver()
    private val fakeUriAppender = FakeUriAppender()
    private val uriAvoidance = MediaUriAvoidance(
        uriAppender = fakeUriAppender,
        externalContentUri = A_EXTERNAL_CONTENT_URI.instance,
    )

    private val useCase = FetchMediaFoldersUseCase(fakeContentResolver.instance, uriAvoidance, aCoroutineDispatchers())

    @Test
    fun `given cursor content, when get folder, then reads unique folders`() = runTest {
        fakeContentResolver.givenFolderQuery().returns(createCursor {
            addFolderRow(rowId = 1, A_BUCKET_ID)
            addFolderRow(rowId = 2, A_BUCKET_ID)
            addFolderRow(rowId = 3, A_SECOND_BUCKET_ID)
        })

        val result = useCase.fetchFolders()

        result shouldBeEqualTo listOf(
            Folder(
                bucketId = A_BUCKET_ID,
                title = A_DISPLAY_NAME,
                thumbnail = fakeUriAppender.get(rowId = 1),
            ),
            Folder(
                bucketId = A_SECOND_BUCKET_ID,
                title = A_DISPLAY_NAME,
                thumbnail = fakeUriAppender.get(rowId = 3),
            ),
        )
        result[0].itemCount shouldBeEqualTo 2
        result[1].itemCount shouldBeEqualTo 1
    }

    private fun CreateCursorScope.addFolderRow(rowId: Long, bucketId: String) {
        addRow(
            MediaStore.Images.Media._ID to rowId,
            MediaStore.Images.Media.BUCKET_ID to bucketId,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME to A_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED to A_DATE_MODIFIED,
        )
    }
}

private fun FakeContentResolver.givenFolderQuery() = this.givenQueryResult(
    A_EXTERNAL_CONTENT_URI.instance,
    arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.DATE_MODIFIED
    ),
    "${isNotPending()} AND ${MediaStore.Images.Media.BUCKET_ID} AND ${MediaStore.Images.Media.MIME_TYPE} NOT LIKE ?",
    arrayOf("%image/svg%"),
    "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} COLLATE NOCASE ASC, ${MediaStore.Images.Media.DATE_MODIFIED} DESC",
)

class FakeUriAppender : (Uri, Long) -> Uri {

    private val uris = mutableMapOf<Long, FakeUri>()

    override fun invoke(uri: Uri, rowId: Long): Uri {
        val fakeUri = FakeUri()
        uris[rowId] = fakeUri
        return fakeUri.instance
    }

    fun get(rowId: Long) = uris[rowId]!!.instance
}