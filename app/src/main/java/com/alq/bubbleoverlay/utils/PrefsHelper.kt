package com.alq.bubbleoverlay.utils

import android.content.Context

class PrefsHelper(context: Context) {
    // val = invariable y solo lectura
    private val sharedPref = context.getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE)

    fun saveImagePath(path: String?) {

    }
}