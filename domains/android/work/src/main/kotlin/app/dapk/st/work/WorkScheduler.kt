package app.dapk.st.work

interface WorkScheduler {

    fun schedule(task: WorkTask)

    data class WorkTask(val jobId: Int, val type: String, val jsonPayload: String)

}