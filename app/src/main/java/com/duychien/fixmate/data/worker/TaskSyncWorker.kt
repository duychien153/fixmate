package com.duychien.fixmate.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.duychien.fixmate.domain.usecase.SyncTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.io.IOException

/**
 * Pushes pending local changes to the remote API. Runs only while the device is
 * online (see [WorkManagerSyncScheduler]) and retries with backoff on transient
 * failures.
 */
@HiltWorker
class TaskSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncTasks: SyncTasksUseCase,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            syncTasks()
            Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "Sync failed, will retry: ${e.message}")
            Result.retry()
        } catch (e: HttpException) {
            if (e.code() in 500..599) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed permanently", e)
            Result.failure()
        }
    }

    private companion object {
        const val TAG = "TaskSyncWorker"
    }
}
