package com.example.recorder.di

import android.content.Context
import androidx.room.Room
import com.example.recorder.data.local.RecorderDatabase
import com.example.recorder.data.repository.RecordingRepositoryImpl
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
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RecorderDatabase =
        Room.databaseBuilder(context, RecorderDatabase::class.java, "recorder.db").build()

    @Provides
    fun provideRecordingDao(database: RecorderDatabase) = database.recordingDao()
}
