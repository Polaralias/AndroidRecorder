package com.example.recorder.data.local.util

import androidx.room.TypeConverter
import java.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromLong(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toLong(instant: Instant?): Long? = instant?.toEpochMilli()
}
