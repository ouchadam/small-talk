package app.dapk.st.core

import kotlinx.coroutines.Job

class JobBag {

    private val jobs = mutableMapOf<String, Job>()

    fun add(key: String, job: Job) {
        jobs[key] = job
    }

    fun replace(key: String, job: Job) {
        jobs[key]?.cancel()
        jobs[key] = job
    }

    fun cancel(key: String) {
        jobs.remove(key)?.cancel()
    }

}