package com.example.recorder.data.local.util

import androidx.room.TypeConverter
import com.example.recorder.domain.model.TranscriptionStatus

class TranscriptionStatusConverter {
    @TypeConverter
    fun fromString(value: String?): TranscriptionStatus? = value?.let { enumValueOf<TranscriptionStatus>(it) }

    @TypeConverter
    fun toString(status: TranscriptionStatus?): String? = status?.name
}
