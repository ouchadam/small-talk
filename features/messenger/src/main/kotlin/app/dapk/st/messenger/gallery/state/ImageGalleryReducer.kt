package app.dapk.st.messenger.gallery.state

import app.dapk.st.core.JobBag
import app.dapk.st.core.Lce
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.messenger.gallery.*
import app.dapk.state.*
import kotlinx.coroutines.launch

fun imageGalleryReducer(
    roomName: String,
    foldersUseCase: FetchMediaFoldersUseCase,
    fetchMediaUseCase: FetchMediaUseCase,
    jobBag: JobBag,
) = shareState {
    combineReducers(
        createPageReducer(roomName),
        createImageGalleryPageReducer(jobBag, foldersUseCase, fetchMediaUseCase),
    )
}

private fun createPageReducer(roomName: String): ReducerFactory<PageContainer<ImageGalleryPage>> = createPageReducer(
    initialPage = SpiderPage(
        route = ImageGalleryPage.Routes.folders,
        label = "Send to $roomName",
        parent = null,
        state = ImageGalleryPage.Folders(Lce.Loading())
    )
)

private fun SharedStateScope<Combined2<PageContainer<ImageGalleryPage>, Unit>>.createImageGalleryPageReducer(
    jobBag: JobBag,
    foldersUseCase: FetchMediaFoldersUseCase,
    fetchMediaUseCase: FetchMediaUseCase
) = createReducer(
    initialState = Unit,

    async(ImageGalleryActions.Visible::class) {
        jobBag.replace("page", coroutineScope.launch {
            val folders = foldersUseCase.fetchFolders()
            dispatch(PageAction.UpdatePage(ImageGalleryPage.Folders(Lce.Content(folders))))
        })
    },

    async(ImageGalleryActions.SelectFolder::class) { action ->
        val page = SpiderPage(
            route = ImageGalleryPage.Routes.files,
            label = getSharedState().state1.page.label,
            parent = ImageGalleryPage.Routes.folders,
            state = ImageGalleryPage.Files(Lce.Loading(), action.folder)
        )
        dispatch(PageAction.GoTo(page))

        jobBag.replace("page", coroutineScope.launch {
            val media = fetchMediaUseCase.getMediaInBucket(action.folder.bucketId)
            dispatch(PageAction.UpdatePage(page.state.copy(content = Lce.Content(media))))
        })
    },

    sideEffect(PageAction.GoTo::class) { action, _ ->
        if (getSharedState().state1.isDifferentPage(action.page)) {
            jobBag.cancel("page")
        }
    }
)
