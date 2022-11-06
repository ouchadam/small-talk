package app.dapk.st.messenger.gallery.state

import app.dapk.st.messenger.gallery.Folder
import app.dapk.state.Action

sealed interface ImageGalleryActions : Action {
    object Visible : ImageGalleryActions
    data class SelectFolder(val folder: Folder) : ImageGalleryActions
}
