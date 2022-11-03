package app.dapk.st.messenger.gallery

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.JobBag
import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.createStateViewModel
import app.dapk.st.messenger.gallery.FetchMediaUseCase.UriAvoidance
import app.dapk.st.messenger.gallery.state.ImageGalleryState
import app.dapk.st.messenger.gallery.state.imageGalleryReducer

class ImageGalleryModule(
    private val contentResolver: ContentResolver,
    private val dispatchers: CoroutineDispatchers,
) : ProvidableModule {

    fun imageGalleryState(roomName: String): ImageGalleryState = createStateViewModel {
        imageGalleryReducer(
            roomName = roomName,
            FetchMediaFoldersUseCase(contentResolver, dispatchers),
            FetchMediaUseCase(
                contentResolver,
                UriAvoidance(
                    uriAppender = { uri, rowId -> ContentUris.withAppendedId(uri, rowId) },
                    externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ),
                dispatchers,
            ),
            JobBag(),
        )
    }

}