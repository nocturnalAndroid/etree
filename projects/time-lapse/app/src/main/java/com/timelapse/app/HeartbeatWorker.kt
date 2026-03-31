package com.timelapse.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class HeartbeatWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val topic = SessionManager.getNtfyTopic(applicationContext)
        if (topic.isBlank()) return Result.success()

        val status = SessionManager.getStatus()
        val message = if (status.isRunning) {
            "✓ TimeLapse alive — ${status.photoCount} photos, ${status.uploadPending} pending upload"
        } else {
            "✓ TimeLapse listener alive — idle"
        }

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://ntfy.sh/$topic")
            .post(message.toRequestBody("text/plain".toMediaType()))
            .header("Title", "TimeLapse Heartbeat")
            .header("Priority", "low")
            .header("Tags", "white_check_mark")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success() else Result.retry()
            }
        } catch (e: IOException) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "timelapse_heartbeat"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(10, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
