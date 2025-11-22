// ReminderSelection.kt
package com.manuelbena.synkron.presentation.models

data class ReminderSelection(
    val isEnabled: Boolean = false,
    val type: ReminderType = ReminderType.NOTIFICATION, // NOTIFICATION o ALARM
    val timeOffset: Long = 0L, // 0 = En el momento, 600000 = 10 min, etc.
    val label: String = "Sin recordatorio"
)

enum class ReminderType {
    NOTIFICATION, ALARM
}