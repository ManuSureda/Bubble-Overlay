package com.alq.bubbleoverlay.utils

import android.net.Uri
import androidx.room.TypeConverter
import com.alq.bubbleoverlay.dao.BubbleShape

class Converters {
    @TypeConverter
    fun uriToString(uri: Uri?): String? = uri?.toString()

    @TypeConverter
    fun stringToUri(uriString: String?): Uri? = uriString?.let { Uri.parse(it) }

    @TypeConverter
    fun fromShape(shape: BubbleShape): String = shape.name

    @TypeConverter
    fun toShape(value: String): BubbleShape {
        return try {
            BubbleShape.valueOf(value)
        } catch (e: IllegalArgumentException) {
            BubbleShape.CIRCLE // Valor por defecto si hay error
        }
    }
}