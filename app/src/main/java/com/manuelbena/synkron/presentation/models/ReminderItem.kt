package com.manuelbena.synkron.presentation.models

enum class ReminderMethod {
    POPUP,  // Notificación estándar
    EMAIL,  // Correo electrónico
    ALARM   // Alarma (Sonido fuerte/Pantalla completa)
}

data class ReminderItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val minutes: Int, // Siempre guardamos en minutos para ser compatible con Google
    val method: ReminderMethod = ReminderMethod.POPUP
) {
    fun getFormattedText(): String {
        return when {
            minutes < 60 -> "$minutes minutos antes"
            minutes == 60 -> "1 hora antes"
            minutes % 60 == 0 && minutes < 1440 -> "${minutes / 60} horas antes"
            minutes == 1440 -> "1 día antes"
            minutes % 1440 == 0 -> "${minutes / 1440} días antes"
            else -> "$minutes minutos antes"
        }
    }
}