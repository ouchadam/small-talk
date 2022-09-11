package app.dapk.st.core

import android.os.Build

fun <T> DeviceMeta.isAtLeastO(block: () -> T, fallback: () -> T = { throw IllegalStateException("not handled") }): T {
    return if (this.apiVersion >= Build.VERSION_CODES.O) block() else fallback()
}

fun DeviceMeta.isAtLeastS() = this.apiVersion >= Build.VERSION_CODES.S

fun DeviceMeta.onAtLeastO(block: () -> Unit) {
    if (this.apiVersion >= Build.VERSION_CODES.O) block()
}

inline fun <T> DeviceMeta.whenPOrHigher(block: () -> T, fallback: () -> T) = whenXOrHigher(Build.VERSION_CODES.P, block, fallback)
inline fun <T> DeviceMeta.whenOOrHigher(block: () -> T, fallback: () -> T) = whenXOrHigher(Build.VERSION_CODES.O, block, fallback)

inline fun <T> DeviceMeta.whenXOrHigher(version: Int, block: () -> T, fallback: () -> T): T {
    return if (this.apiVersion >= version) block() else fallback()
}

