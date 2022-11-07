package app.dapk.st.core

import kotlinx.coroutines.Job
import kotlin.reflect.KClass

class JobBag {

    private val jobs = mutableMapOf<String, Job>()

    fun replace(key: String, job: Job) {
        jobs[key]?.cancel()
        jobs[key] = job
    }

    fun replace(key: KClass<*>, job: Job) {
        jobs[key.java.canonicalName]?.cancel()
        jobs[key.java.canonicalName] = job
    }

    fun cancel(key: String) {
        jobs.remove(key)?.cancel()
    }

    fun cancel(key: KClass<*>) {
        jobs.remove(key.java.canonicalName)?.cancel()
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
    }

}