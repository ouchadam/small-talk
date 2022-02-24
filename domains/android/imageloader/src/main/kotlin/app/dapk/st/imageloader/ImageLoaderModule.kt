package app.dapk.st.imageloader

import android.content.Context
import app.dapk.st.core.extensions.unsafeLazy

class ImageLoaderModule(
    private val context: Context,
) {

    private val imageLoader by unsafeLazy { CoilImageLoader(context) }

    private val cachedIcons by unsafeLazy { CachedIcons(imageLoader) }

    fun iconLoader(): IconLoader = cachedIcons

}