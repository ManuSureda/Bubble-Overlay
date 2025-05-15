package com.alq.bubbleoverlay.utils

import android.net.Uri
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun uriToString(uri: Uri?): String? = uri?.toString()

    @TypeConverter
    fun stringToUri(uriString: String?): Uri? = uriString?.let { Uri.parse(it) }

}