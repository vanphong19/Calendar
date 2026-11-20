package com.app.calendar.images

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.app.calendar.core.domain.holiday.HolidayCalendar
import com.app.calendar.core.domain.repository.DailyContentRepository
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.model.dailyContentRuleIds
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/**
 * Keeps a rolling window of daily images in the disk cache so pages have their picture even
 * without a connection later. Runs only on unmetered networks (Wi-Fi) with enough battery and
 * storage, and only downloads what is not cached yet (Coil's disk cache answers repeats).
 */
@HiltWorker
class PrefetchDailyImagesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dailyContent: DailyContentRepository,
    private val holidayCalendar: HolidayCalendar,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!preferences.preferences.first().dailyImagesEnabled) return Result.success()
        val today = LocalDate.now(clock)
        val urls = LinkedHashSet<String>()

        for (offset in 0 until WINDOW_DAYS) {
            val date = today.plusDays(offset.toLong())
            val holidayIds = holidayCalendar.year(date.year).holidaysOn(date).dailyContentRuleIds()
            dailyContent.contentFor(date, holidayIds).image?.remoteUrl?.let(urls::add)
        }
        // Wall-calendar covers for this month and the next.
        listOf(YearMonth.from(today), YearMonth.from(today).plusMonths(1)).forEach { month ->
            dailyContent.monthCover(month).image?.remoteUrl?.let(urls::add)
        }

        val loader = SingletonImageLoader.get(applicationContext)
        var failures = 0
        urls.forEach { url ->
            if (isStopped) return Result.retry()
            val request = ImageRequest.Builder(applicationContext)
                .data(url) // Same key as the UI uses, so the page finds it in the disk cache.
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            if (loader.execute(request) is ErrorResult) failures++
        }
        Log.i(TAG, "Prefetched ${urls.size - failures}/${urls.size} daily images")
        // Partial failures are retried on the next run; only retry now if nothing worked.
        return if (failures == urls.size && urls.isNotEmpty()) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "LichPrefetch"
        const val WINDOW_DAYS = 30
        private const val UNIQUE_PERIODIC = "prefetch-daily-images-periodic"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        /**
         * Daily top-up of the window. A new periodic request runs its first pass as soon as
         * Wi-Fi is available, so no separate one-off request is needed.
         */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PrefetchDailyImagesWorker>(1, TimeUnit.DAYS).setConstraints(constraints).build(),
            )
        }
    }
}
