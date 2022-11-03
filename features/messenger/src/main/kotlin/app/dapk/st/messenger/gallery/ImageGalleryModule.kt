package app.dapk.st.messenger.gallery

import android.content.ContentResolver
import app.dapk.st.core.*
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
            FetchMediaUseCase(contentResolver, dispatchers),
            JobBag(),
        )
    }

}