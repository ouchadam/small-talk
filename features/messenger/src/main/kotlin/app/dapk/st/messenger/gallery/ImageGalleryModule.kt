package app.dapk.st.messenger.gallery

import android.content.ContentResolver
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.ProvidableModule

class ImageGalleryModule(
    private val contentResolver: ContentResolver,
    private val dispatchers: CoroutineDispatchers,
) : ProvidableModule {

    fun imageGalleryViewModel() = ImageGalleryViewModel(
        FetchMediaFoldersUseCase(contentResolver, dispatchers),
        FetchMediaUseCase(contentResolver, dispatchers),
    )

}