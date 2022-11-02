package app.dapk.st.messenger.gallery.state

import app.dapk.st.core.Lce
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.messenger.gallery.FetchMediaFoldersUseCase
import app.dapk.st.messenger.gallery.FetchMediaUseCase
import app.dapk.st.core.page.PageContainer
import app.dapk.state.Combined2
import fake.FakeJobBag
import io.mockk.mockk
import org.junit.Test
import test.testReducer

private const val A_ROOM_NAME = "a room name"

class ImageGalleryReducerTest {

    private val fakeJobBag = FakeJobBag()

    private val runReducerTest = testReducer { _: (Unit) -> Unit ->
        imageGalleryReducer(
            A_ROOM_NAME,
            FakeFetchMediaFoldersUseCase().instance,
            FakeFetchMediaUseCase().instance,
            fakeJobBag.instance,
        )
    }

    @Test
    fun `initial state is folders page`() = runReducerTest {
        assertInitialState(
            Combined2(
                state1 = PageContainer(
                    SpiderPage(
                        route = ImageGalleryPage.Routes.folders,
                        label = "Send to $A_ROOM_NAME",
                        parent = null,
                        state = ImageGalleryPage.Folders(Lce.Loading())
                    )
                ),
                state2 = Unit
            )
        )
    }

}

class FakeFetchMediaFoldersUseCase {
    val instance = mockk<FetchMediaFoldersUseCase>()
}

class FakeFetchMediaUseCase {
    val instance = mockk<FetchMediaUseCase>()
}