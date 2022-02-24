package app.dapk.st.matrix.message

interface BackgroundScheduler {

    fun schedule(key: String, task: Task)

    data class Task(val type: String, val jsonPayload: String)
}

