package app.dapk.st.core

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O, lambda = 0)
fun <T> DeviceMeta.isAtLeastO(block: () -> T, fallback: () -> T = { throw IllegalStateException("not handled") }): T {
    return if (this.apiVersion >= Build.VERSION_CODES.O) block() else fallback()
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun DeviceMeta.isAtLeastS() = this.apiVersion >= Build.VERSION_CODES.S

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O, lambda = 0)
fun DeviceMeta.onAtLeastO(block: () -> Unit) {
    whenXOrHigher(Build.VERSION_CODES.O, block, fallback = {})
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q, lambda = 0)
fun DeviceMeta.onAtLeastQ(block: () -> Unit) {
    whenXOrHigher(Build.VERSION_CODES.Q, block, fallback = {})
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R, lambda = 0)
fun DeviceMeta.onAtLeastR(block: () -> Unit) {
    whenXOrHigher(Build.VERSION_CODES.R, block, fallback = {})
}

inline fun <T> DeviceMeta.whenPOrHigher(block: () -> T, fallback: () -> T) = whenXOrHigher(Build.VERSION_CODES.P, block, fallback)
inline fun <T> DeviceMeta.whenOOrHigher(block: () -> T, fallback: () -> T) = whenXOrHigher(Build.VERSION_CODES.O, block, fallback)

@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun <T> DeviceMeta.whenXOrHigher(version: Int, block: () -> T, fallback: () -> T): T {
    return if (this.apiVersion >= version) block() else fallback()
}

