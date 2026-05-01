package com.manuelbena.synkron.presentation.util.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatDate(): String {
    return SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(this))
}

fun Long.formatTime(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))
}

fun Long.toDurationString(): String {
    if (this == 0L) return ""

    val hours = this / 60
    val minutes = this % 60

    return when {
        hours > 0 && minutes > 0 -> "$hours h $minutes min"
        hours > 0 -> "$hours h"
        else -> "$minutes min"
    }
}