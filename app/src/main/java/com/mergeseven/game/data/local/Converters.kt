package com.mergeseven.game.data.local

import androidx.room.TypeConverter

/**
 * Room type converters. Kept deliberately small — anything more structured than a scalar set
 * belongs in its own table or in an explicitly versioned snapshot blob.
 */
class Converters {

    @TypeConverter
    fun intSetToString(value: Set<Int>?): String =
        value.orEmpty().sorted().joinToString(separator = ",")

    @TypeConverter
    fun stringToIntSet(value: String?): Set<Int> =
        value?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            .orEmpty()
}
