package com.manuelbena.synkron.presentation.models

import java.util.UUID

// 1. ENUM ACTUALIZADO:
// - Cambiamos POPUP por NOTIFICATION (es el término estándar en Android).
// - Cambiamos EMAIL por WHATSAPP (según tu requerimiento de negocio).
enum class ReminderMethod { NOTIFICATION, WHATSAPP, ALARM }

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),

    // Hora objetivo de la tarea (Cuándo ocurre el evento)
    val hour: Int,
    val minute: Int,

    // Configuración del aviso
    val method: ReminderMethod,

    // 2. NUEVO CAMPO: Antelación
    // Guardamos la antelación siempre en MINUTOS para mantener la consistencia.
    // Si el usuario elige "1 día antes", guardas 1440 (24 * 60).
    // Si elige "10 minutos antes", guardas 10.
    val offsetMinutes: Int = 0,

    // Campos de UI (Helpers para pintar rápido sin recalcular)
    val displayTime: String,    // Ej: "09:00"
    val message: String         // Ej: "Llamar a Sergio"
)