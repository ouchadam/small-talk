package app.dapk.st.matrix.message

import app.dapk.st.matrix.common.JsonString

interface BackgroundScheduler {

    fun schedule(key: String, task: Task)

    data class Task(val type: String, val jsonPayload: JsonString)
}

