package com.manuelbena.synkron.presentation.models

import java.util.UUID

enum class ReminderMethod { POPUP, EMAIL, ALARM }

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val minutes: Int,           // Minutos para la API de Google
    val method: ReminderMethod, // Tipo
    val displayTime: String,    // EJ: "09:00" (Necesario para pintar la UI)
    val message: String         // EJ: "Llamar a..." (Necesario para pintar la UI)
)