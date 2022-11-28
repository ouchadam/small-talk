package app.dapk.st.messenger.gallery.state

import app.dapk.st.core.JobBag
import app.dapk.st.core.Lce
import app.dapk.st.messenger.gallery.FetchMediaFoldersUseCase
import app.dapk.st.messenger.gallery.FetchMediaUseCase
import app.dapk.state.SpiderPage
import app.dapk.state.async
import app.dapk.state.createReducer
import app.dapk.state.page.PageAction
import app.dapk.state.page.PageStateChange
import app.dapk.state.page.createPageReducer
import app.dapk.state.page.withPageContext
import app.dapk.state.sideEffect
import kotlinx.coroutines.launch

fun imageGalleryReducer(
    roomName: String,
    foldersUseCase: FetchMediaFoldersUseCase,
    fetchMediaUseCase: FetchMediaUseCase,
    jobBag: JobBag,
) = createPageReducer(
    initialPage = SpiderPage<ImageGalleryPage>(
        route = ImageGalleryPage.Routes.folders,
        label = "Send to $roomName",
        parent = null,
        state = ImageGalleryPage.Folders(Lce.Loading())
    ),
    factory = {
        createReducer(
            initialState = Unit,

            async(ImageGalleryActions.Visible::class) {
                jobBag.replace(ImageGalleryPage.Folders::class, coroutineScope.launch {
                    val folders = foldersUseCase.fetchFolders()
                    withPageContext<ImageGalleryPage.Folders> {
                        pageDispatch(PageStateChange.UpdatePage(it.copy(content = Lce.Content(folders))))
                    }
                })
            },

            async(ImageGalleryActions.SelectFolder::class) { action ->
                val page = SpiderPage(
                    route = ImageGalleryPage.Routes.files,
                    label = rawPage().label,
                    parent = ImageGalleryPage.Routes.folders,
                    state = ImageGalleryPage.Files(Lce.Loading(), action.folder)
                )
                dispatch(PageAction.GoTo(page))

                jobBag.replace(ImageGalleryPage.Files::class, coroutineScope.launch {
                    val media = fetchMediaUseCase.getMediaInBucket(action.folder.bucketId)
                    withPageContext<ImageGalleryPage.Files> {
                        pageDispatch(PageStateChange.UpdatePage(it.copy(content = Lce.Content(media))))
                    }
                })
            },

            sideEffect(PageStateChange.ChangePage::class) { action, _ ->
                jobBag.cancel(action.previous.state::class)
            },
        )
    }
)
