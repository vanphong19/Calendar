package com.app.calendar

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.app.calendar.core.data.reminder.ReminderNotifier
import com.app.calendar.core.data.reminder.RescheduleRemindersWorker
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class LichApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.ensureChannel(this)
        RescheduleRemindersWorker.ensurePeriodic(this)
        RescheduleRemindersWorker.enqueueNow(this)
    }

    /** One image loader for the whole app: bounded memory and a 100 MB disk cache for daily images. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.15).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("daily_images")).maxSizeBytes(100L * 1024 * 1024).build() }
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder()
                                // Wikimedia asks clients to identify themselves; no personal data.
                                .addInterceptor { chain ->
                                    chain.proceed(chain.request().newBuilder().header("User-Agent", "LichViet/1.0 (Android calendar app)").build())
                                }
                                .build()
                        },
                    ),
                )
            }
            .crossfade(true)
            .build()
}
