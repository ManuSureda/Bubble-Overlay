package com.alq.bubbleoverlay.utils

import android.content.res.Resources

fun Int.dpToPx(resources: Resources): Int =
    (this * resources.displayMetrics.density).toInt()
