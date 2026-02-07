package com.zeynbakers.order_management_system.core.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }
}
