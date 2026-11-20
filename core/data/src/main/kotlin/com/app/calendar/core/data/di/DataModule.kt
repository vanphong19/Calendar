package com.app.calendar.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.app.calendar.core.data.assets.AssetReader
import com.app.calendar.core.data.db.EventDao
import com.app.calendar.core.data.db.LichDatabase
import com.app.calendar.core.data.db.ScheduledAlarmDao
import com.app.calendar.core.data.reminder.WorkManagerReminderScheduler
import com.app.calendar.core.data.repository.AndroidNetworkMonitor
import com.app.calendar.core.data.repository.AssetDailyContentRepository
import com.app.calendar.core.data.repository.AssetHolidayRepository
import com.app.calendar.core.data.repository.DataStorePreferencesRepository
import com.app.calendar.core.data.repository.OfflineEventRepository
import com.app.calendar.core.domain.repository.DailyContentRepository
import com.app.calendar.core.domain.repository.EventRepository
import com.app.calendar.core.domain.repository.HolidayRepository
import com.app.calendar.core.domain.repository.NetworkMonitor
import com.app.calendar.core.domain.repository.PreferencesRepository
import com.app.calendar.core.domain.repository.ReminderScheduler
import com.app.calendar.core.lunar.LunarCalendar
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
internal object DataProvidesModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): LichDatabase =
        Room.databaseBuilder(context, LichDatabase::class.java, LichDatabase.NAME).build()

    @Provides
    fun eventDao(db: LichDatabase): EventDao = db.eventDao()

    @Provides
    fun scheduledAlarmDao(db: LichDatabase): ScheduledAlarmDao = db.scheduledAlarmDao()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.settingsDataStore

    @Provides
    @Singleton
    fun lunarCalendar(): LunarCalendar = LunarCalendar()

    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Named(AssetReader.IO_DISPATCHER)
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindsModule {
    @Binds
    @Singleton
    abstract fun events(impl: OfflineEventRepository): EventRepository

    @Binds
    @Singleton
    abstract fun preferences(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds
    abstract fun holidays(impl: AssetHolidayRepository): HolidayRepository

    @Binds
    abstract fun dailyContent(impl: AssetDailyContentRepository): DailyContentRepository

    @Binds
    abstract fun reminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler

    @Binds
    abstract fun networkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}
