package app.dapk.st.messenger.gallery

import android.provider.MediaStore
import fake.FakeContentResolver
import fake.FakeUri
import fake.createCursor
import fixture.CoroutineDispatchersFixture.aCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_EXTERNAL_CONTENT_URI = FakeUri()
private val ROW_URI = FakeUri()
private const val A_BUCKET_ID = "a-bucket-id"
private const val A_ROW_ID = 20L
private const val A_MIME_TYPE = "image/png"
private const val A_DATE_MODIFIED = 5000L
private const val A_SIZE = 1000L
private const val A_NORMAL_ORIENTATION = 0
private const val AN_INVERTED_ORIENTATION = 90
private const val A_WIDTH = 250
private const val A_HEIGHT = 750

class FetchMediaUseCaseTest {

    private val fakeContentResolver = FakeContentResolver()
    private val uriAvoidance = MediaUriAvoidance(
        uriAppender = { _, _ -> ROW_URI.instance },
        externalContentUri = A_EXTERNAL_CONTENT_URI.instance,
    )

    private val useCase = FetchMediaUseCase(fakeContentResolver.instance, uriAvoidance, aCoroutineDispatchers())

    @Test
    fun `given cursor content, when get media for bucket, then reads media`() = runTest {
        fakeContentResolver.givenMediaQuery().returns(createCursor {
            addRow(
                MediaStore.Images.Media._ID to A_ROW_ID,
                MediaStore.Images.Media.MIME_TYPE to A_MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED to A_DATE_MODIFIED,
                MediaStore.Images.Media.ORIENTATION to A_NORMAL_ORIENTATION,
                MediaStore.Images.Media.WIDTH to A_WIDTH,
                MediaStore.Images.Media.HEIGHT to A_HEIGHT,
                MediaStore.Images.Media.SIZE to A_SIZE,
            )
        })

        val result = useCase.getMediaInBucket(A_BUCKET_ID)

        result shouldBeEqualTo listOf(
            Media(
                id = A_ROW_ID,
                uri = ROW_URI.instance,
                mimeType = A_MIME_TYPE,
                width = A_WIDTH,
                height = A_HEIGHT,
                size = A_SIZE,
                dateModifiedEpochMillis = A_DATE_MODIFIED
            )
        )
    }

    @Test
    fun `given cursor content with 90 degree orientation, when get media for bucket, then reads media with inverted width and height`() = runTest {
        fakeContentResolver.givenMediaQuery().returns(createCursor {
            addRow(
                MediaStore.Images.Media._ID to A_ROW_ID,
                MediaStore.Images.Media.MIME_TYPE to A_MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED to A_DATE_MODIFIED,
                MediaStore.Images.Media.ORIENTATION to AN_INVERTED_ORIENTATION,
                MediaStore.Images.Media.WIDTH to A_WIDTH,
                MediaStore.Images.Media.HEIGHT to A_HEIGHT,
                MediaStore.Images.Media.SIZE to A_SIZE,
            )
        })

        val result = useCase.getMediaInBucket(A_BUCKET_ID)

        result shouldBeEqualTo listOf(
            Media(
                id = A_ROW_ID,
                uri = ROW_URI.instance,
                mimeType = A_MIME_TYPE,
                width = A_HEIGHT,
                height = A_WIDTH,
                size = A_SIZE,
                dateModifiedEpochMillis = A_DATE_MODIFIED
            )
        )
    }
}

private fun FakeContentResolver.givenMediaQuery() = this.givenQueryResult(
    A_EXTERNAL_CONTENT_URI.instance,
    arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.ORIENTATION,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.SIZE
    ),
    MediaStore.Images.Media.BUCKET_ID + " = ? AND " + isNotPending() + " AND " + MediaStore.Images.Media.MIME_TYPE + " NOT LIKE ?",
    arrayOf(A_BUCKET_ID, "%image/svg%"),
    MediaStore.Images.Media.DATE_MODIFIED + " DESC",
)
