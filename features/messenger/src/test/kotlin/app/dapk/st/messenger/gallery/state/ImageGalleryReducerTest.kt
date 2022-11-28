package app.dapk.st.messenger.gallery.state

import android.net.Uri
import app.dapk.st.core.Lce
import app.dapk.st.messenger.gallery.FetchMediaFoldersUseCase
import app.dapk.st.messenger.gallery.FetchMediaUseCase
import app.dapk.st.messenger.gallery.Folder
import app.dapk.st.messenger.gallery.Media
import app.dapk.state.Combined2
import app.dapk.state.SpiderPage
import app.dapk.state.page.PageAction
import app.dapk.state.page.PageContainer
import app.dapk.state.page.PageStateChange
import fake.FakeJobBag
import fake.FakeUri
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Test
import test.assertOnlyDispatches
import test.delegateReturn
import test.testReducer

private const val A_ROOM_NAME = "a room name"
private val A_FOLDER = Folder(
    bucketId = "a-bucket-id",
    title = "a title",
    thumbnail = FakeUri().instance,
)
private val A_MEDIA_RESULT = listOf(aMedia())
private val A_FOLDERS_RESULT = listOf(aFolder())
private val AN_INITIAL_FILES_PAGE = SpiderPage(
    route = ImageGalleryPage.Routes.files,
    label = "Send to $A_ROOM_NAME",
    parent = ImageGalleryPage.Routes.folders,
    state = ImageGalleryPage.Files(Lce.Loading(), A_FOLDER)
)

private val AN_INITIAL_FOLDERS_PAGE = SpiderPage(
    route = ImageGalleryPage.Routes.folders,
    label = "Send to $A_ROOM_NAME",
    parent = null,
    state = ImageGalleryPage.Folders(Lce.Loading())
)

class ImageGalleryReducerTest {

    private val fakeJobBag = FakeJobBag()
    private val fakeFetchMediaFoldersUseCase = FakeFetchMediaFoldersUseCase()
    private val fakeFetchMediaUseCase = FakeFetchMediaUseCase()

    private val runReducerTest = testReducer { _: (Unit) -> Unit ->
        imageGalleryReducer(
            A_ROOM_NAME,
            fakeFetchMediaFoldersUseCase.instance,
            fakeFetchMediaUseCase.instance,
            fakeJobBag.instance,
        )
    }

    @Test
    fun `initial state is folders page`() = runReducerTest {
        assertInitialState(pageState(AN_INITIAL_FOLDERS_PAGE))
    }

    @Test
    fun `when Visible, then updates Folders content`() = runReducerTest {
        fakeJobBag.instance.expect { it.replace(ImageGalleryPage.Folders::class, any()) }
        fakeFetchMediaFoldersUseCase.givenFolders().returns(A_FOLDERS_RESULT)

        reduce(ImageGalleryActions.Visible)

        assertOnlyDispatches(
            PageStateChange.UpdatePage(AN_INITIAL_FOLDERS_PAGE.state.copy(content = Lce.Content(A_FOLDERS_RESULT)))
        )
    }

    @Test
    fun `when SelectFolder, then goes to Folder page and fetches content`() = runReducerTest {
        fakeJobBag.instance.expect { it.replace(ImageGalleryPage.Files::class, any()) }
        fakeFetchMediaUseCase.givenMedia(A_FOLDER.bucketId).returns(A_MEDIA_RESULT)
        val goToFolderPage = PageAction.GoTo(AN_INITIAL_FILES_PAGE)
        actionSideEffect(goToFolderPage) { pageState(goToFolderPage.page) }

        reduce(ImageGalleryActions.SelectFolder(A_FOLDER))

        assertOnlyDispatches(
            goToFolderPage,
            PageStateChange.UpdatePage(goToFolderPage.page.state.copy(content = Lce.Content(A_MEDIA_RESULT)))
        )
    }

    @Test
    fun `when ChangePage, then cancels previous page jobs`() = runReducerTest {
        fakeJobBag.instance.expect { it.cancel(ImageGalleryPage.Folders::class) }

        reduce(PageStateChange.ChangePage(previous = AN_INITIAL_FOLDERS_PAGE, newPage = AN_INITIAL_FILES_PAGE))

        assertOnlyStateChange(pageState(AN_INITIAL_FILES_PAGE))
    }
}

private fun <P> pageState(page: SpiderPage<out P>) = Combined2(PageContainer(page), Unit)

class FakeFetchMediaFoldersUseCase {
    val instance = mockk<FetchMediaFoldersUseCase>()

    fun givenFolders() = coEvery { instance.fetchFolders() }.delegateReturn()
}

class FakeFetchMediaUseCase {
    val instance = mockk<FetchMediaUseCase>()

    fun givenMedia(bucketId: String) = coEvery { instance.getMediaInBucket(bucketId) }.delegateReturn()
}

fun aMedia(
    id: Long = 1L,
    uri: Uri = FakeUri().instance,
    mimeType: String = "image/png",
    width: Int = 100,
    height: Int = 250,
    size: Long = 1000L,
    dateModifiedEpochMillis: Long = 5000L,
) = Media(id, uri, mimeType, width, height, size, dateModifiedEpochMillis)

fun aFolder(
    bucketId: String = "a-bucket-id",
    title: String = "a title",
    thumbnail: Uri = FakeUri().instance,
) = Folder(bucketId, title, thumbnail)