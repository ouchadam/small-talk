package test.impl

import app.dapk.st.core.extensions.ErrorTracker

class PrintingErrorTracking(private val prefix: String) : ErrorTracker {
    override fun track(throwable: Throwable, extra: String) {
        println("$prefix ${throwable.message}")
        throwable.printStackTrace()
    }
}