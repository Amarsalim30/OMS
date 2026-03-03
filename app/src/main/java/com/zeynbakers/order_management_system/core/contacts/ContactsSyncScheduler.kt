package com.zeynbakers.order_management_system.core.contacts

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ContactsSyncScheduler {
    private const val UNIQUE_DAILY_WORK = "daily_contacts_sync"

    fun ensureScheduled(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<ContactsSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(defaultConstraints())
                .addTag(UNIQUE_DAILY_WORK)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_DAILY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun defaultConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
    }
}
