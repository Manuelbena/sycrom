package com.manuelbena.synkron.presentation.util

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.presentation.models.CategoryType
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- Extensions for String (Categories) ---

fun String.getCategoryColor(): Int {
    return CategoryType.entries.find {
        it.title.equals(this, ignoreCase = true) || it.id.equals(this, ignoreCase = true)
    }?.colorRes ?: R.color.cat_default
}

fun String.getCategoryIcon(): Int {
    return CategoryType.entries.find {
        it.title.equals(this, ignoreCase = true) || it.id.equals(this, ignoreCase = true)
    }?.iconRes ?: R.drawable.ic_other
}

fun String.getName(): String {
    return CategoryType.entries.find {
        it.title.equals(this, ignoreCase = true) || it.id.equals(this, ignoreCase = true)
    }?.title ?: this
}

fun String.getCategoryGradientDrawable(): Int {
    return when (this.lowercase(Locale.ROOT)) {
        "trabajo", "work" -> R.drawable.bg_work_gradient
        "personal" -> R.drawable.bg_personal_gradient
        "salud", "health" -> R.drawable.bg_healt_gradient
        "dinero", "finance" -> R.drawable.bg_money_gradient
        "estudios", "study" -> R.drawable.bg_studyl_gradient
        else -> R.drawable.bg_header
    }
}

// --- Extensions for Priority ---

fun String.getPriorityColor(): Int {
    return when (this.lowercase(Locale.ROOT)) {
        "alta", "high" -> R.color.priority_high
        "media", "medium" -> R.color.priority_medium
        "baja", "low" -> R.color.priority_low
        else -> R.color.priority_default
    }
}

// --- Extensions for GoogleEventDateTime ---

fun GoogleEventDateTime?.toHourString(): String {
    if (this?.dateTime == null) return "--:--"
    val date = Date(this.dateTime)
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

fun GoogleEventDateTime?.toLocalDate(): LocalDate {
    if (this == null) return LocalDate.now()
    if (this.dateTime != null) {
        return Instant.ofEpochMilli(this.dateTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    if (!this.date.isNullOrEmpty()) {
        return try {
            LocalDate.parse(this.date)
        } catch (_: Exception) {
            LocalDate.now()
        }
    }
    return LocalDate.now()
}

fun GoogleEventDateTime?.toCalendar(): Calendar? {
    if (this == null) return null
    val cal = Calendar.getInstance()
    if (this.dateTime != null) {
        cal.timeInMillis = this.dateTime
        return cal
    }
    if (!this.date.isNullOrEmpty()) {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateObj = format.parse(this.date)
            if (dateObj != null) {
                cal.time = dateObj
                return cal
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

// --- Top-level helper functions ---

fun getDurationInMinutes(start: GoogleEventDateTime?, end: GoogleEventDateTime?): Int {
    if (start?.dateTime == null || end?.dateTime == null) return 0
    val diff = end.dateTime - start.dateTime
    return (diff / (1000 * 60)).toInt()
}

// --- CarouselScrollListener ---

class CarouselScrollListener : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        val childCount = recyclerView.childCount
        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i)
            val childCenter = (child.left + child.right) / 2f
            val recyclerViewCenter = recyclerView.width / 2f
            val distanceFromCenter = kotlin.math.abs(recyclerViewCenter - childCenter)
            val scale = 1f - (distanceFromCenter / recyclerView.width) * 0.15f
            child.scaleX = scale
            child.scaleY = scale
        }
    }
}
