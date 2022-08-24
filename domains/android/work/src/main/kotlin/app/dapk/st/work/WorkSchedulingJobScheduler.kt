package app.dapk.st.work

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobWorkItem
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

internal class WorkSchedulingJobScheduler(
    private val context: Context,
) : WorkScheduler {

    private val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

    override fun schedule(task: WorkScheduler.WorkTask) {
        val job = JobInfo.Builder(100, ComponentName(context, WorkAndroidService::class.java))
            .setMinimumLatency(1)
            .setOverrideDeadline(1)
            .setBackoffCriteria(1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val item = JobWorkItem(
                Intent()
                    .putExtra("task-type", task.type)
                    .putExtra("task-payload", task.jsonPayload)
            )
            jobScheduler.enqueue(job, item)
        } else {
            job.extras.putString("task-type", task.type)
            job.extras.putString("task-payload", task.jsonPayload)
            jobScheduler.schedule(job)
        }
    }
}
