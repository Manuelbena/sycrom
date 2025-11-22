package com.manuelbena.synkron.presentation.models

import java.util.UUID

enum class ReminderMethod { POPUP, EMAIL, ALARM }

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val minutes: Int,           // Para la lógica interna (Google API)
    val method: ReminderMethod, // Tipo de aviso
    val displayTime: String,    // NUEVO: Para mostrar "09:00" en la UI
    val message: String         // NUEVO: Para mostrar "Llamar a Sergio" en la UI
)