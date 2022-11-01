package app.dapk.st.messenger.gallery.state

import app.dapk.st.core.Lce
import app.dapk.st.core.State
import app.dapk.st.design.components.Route
import app.dapk.st.messenger.gallery.Folder
import app.dapk.st.messenger.gallery.Media
import app.dapk.st.messenger.gallery.PageContainer
import app.dapk.state.Combined2

typealias ImageGalleryState = State<Combined2<PageContainer<ImageGalleryPage>, Unit>, Unit>

sealed interface ImageGalleryPage {
    data class Folders(val content: Lce<List<Folder>>) : ImageGalleryPage
    data class Files(val content: Lce<List<Media>>, val folder: Folder) : ImageGalleryPage

    object Routes {
        val folders = Route<Folders>("Folders")
        val files = Route<Files>("Files")
    }
}
