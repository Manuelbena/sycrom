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