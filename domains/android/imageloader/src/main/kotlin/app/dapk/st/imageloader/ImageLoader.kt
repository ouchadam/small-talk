package app.dapk.st.imageloader

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import coil.transform.Transformation
import coil.load as coilLoad

interface ImageLoader {

    suspend fun load(url: String, transformation: Transformation? = null): Drawable?

}

interface IconLoader {

    suspend fun load(url: String): Icon?

}


class CachedIcons(private val imageLoader: ImageLoader) : IconLoader {

    private val circleCrop = CircleCropTransformation()
    private val cache = mutableMapOf<String, Icon?>()

    override suspend fun load(url: String): Icon? {
        return cache.getOrPut(url) {
            imageLoader.load(url, transformation = circleCrop)?.toBitmap()?.let {
                Icon.createWithBitmap(it)
            }
        }
    }
}


internal class CoilImageLoader(private val context: Context) : ImageLoader {

    private val coil = context.imageLoader

    override suspend fun load(url: String, transformation: Transformation?): Drawable? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .let {
                when (transformation) {
                    null -> it
                    else -> it.transformations(transformation)
                }
            }
            .build()
        return coil.execute(request).drawable
    }
}

fun ImageView.load(url: String) {
    this.coilLoad(url)
}