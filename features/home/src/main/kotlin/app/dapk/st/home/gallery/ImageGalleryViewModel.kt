package app.dapk.st.home.gallery

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.Lce
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.home.ImageGalleryEvent
import app.dapk.st.home.ImageGalleryPage
import app.dapk.st.home.ImageGalleryState
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ImageGalleryViewModel(
    private val foldersUseCase: FetchMediaFoldersUseCase,
    private val fetchMediaUseCase: FetchMediaUseCase,
) : DapkViewModel<ImageGalleryState, ImageGalleryEvent>(
    initialState = ImageGalleryState(
        page = SpiderPage(
            route = ImageGalleryPage.Routes.folders,
            label = "",
            parent = null,
            state = ImageGalleryPage.Folders(Lce.Loading())
        )
    )
) {

    private var currentPageJob: Job? = null

    fun start() {
        currentPageJob?.cancel()
        currentPageJob = viewModelScope.launch {
            val folders = foldersUseCase.fetchFolders()
            updatePageState<ImageGalleryPage.Folders> { copy(content = Lce.Content(folders)) }
        }

    }

    fun goTo(page: SpiderPage<out ImageGalleryPage>) {
        currentPageJob?.cancel()
        updateState { copy(page = page) }
    }

    fun selectFolder(folder: Folder) {
        currentPageJob?.cancel()

        updateState {
            copy(
                page = SpiderPage(
                    route = ImageGalleryPage.Routes.files,
                    label = page.label,
                    parent = ImageGalleryPage.Routes.folders,
                    state = ImageGalleryPage.Files(Lce.Loading())
                )
            )
        }

        currentPageJob = viewModelScope.launch {
            val media = fetchMediaUseCase.getMediaInBucket(folder.bucketId)
            updatePageState<ImageGalleryPage.Files> {
                copy(content = Lce.Content(media))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified S : ImageGalleryPage> updatePageState(crossinline block: S.() -> S) {
        val page = state.page
        val currentState = page.state
        require(currentState is S)
        updateState { copy(page = (page as SpiderPage<S>).copy(state = block(page.state))) }
    }

}