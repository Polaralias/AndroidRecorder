package com.example.recorder.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.recorder.data.drive.BackupPreferencesDataSource
import com.example.recorder.data.drive.DriveBackupRepositoryImpl
import com.example.recorder.data.local.MIGRATION_1_2
import com.example.recorder.data.local.RecorderDatabase
import com.example.recorder.data.repository.RecordingRepositoryImpl
import com.example.recorder.domain.repository.DriveBackupRepository
import com.example.recorder.domain.repository.RecordingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    @Singleton
    abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindDriveBackupRepository(impl: DriveBackupRepositoryImpl): DriveBackupRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RecorderDatabase =
        Room.databaseBuilder(context, RecorderDatabase::class.java, "recorder.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideRecordingDao(database: RecorderDatabase) = database.recordingDao()

    @Provides
    @Singleton
    fun provideBackupPreferencesStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("backup_prefs") }
        )

    @Provides
    @Singleton
    fun provideBackupPreferences(dataStore: DataStore<Preferences>): BackupPreferencesDataSource =
        BackupPreferencesDataSource(dataStore)
}
