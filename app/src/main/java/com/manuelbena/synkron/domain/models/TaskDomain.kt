package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modelo de dominio principal para una Tarea, unificado con el
 * recurso 'Event' de la API de Google Calendar.
 *
 * @property id ID local de Synkrón (no existe en Google).
 * @property subTasks Lógica interna de Synkrón (no existe en Google Events).
 * @property typeTask Categoría interna de Synkrón (no existe en Google).
 * @property priority Prioridad interna de Synkrón (no existe en Google).
 * @property isActive, isDone, isDeleted, isArchived, isPinned Banderas internas.
 *
 * --- CAMPOS UNIFICADOS CON GOOGLE CALENDAR (API v3) ---
 *
 * @property summary El título del evento.
 * @property description La descripción (opcional).
 * @property location La ubicación (opcional).
 * @property colorId El ID del color (del "1" al "11").
 * @property start Objeto de fecha/hora de inicio.
 * @property end Objeto de fecha/hora de fin.
 * @property attendees Lista de objetos de invitados.
 * @property recurrence Lista de reglas de recurrencia (RRULE).
 * @property reminders Objeto que contiene la lista de recordatorios.
 * @property transparency "opaque" (Ocupado) o "transparent" (Disponible).
 * @property conferenceLink (Campo simplificado) Enlace a la videoconferencia.
 */
@Parcelize
data class TaskDomain(
    // --- Campos Internos de Synkrón ---
    val id: Long = 0L,
    val subTasks: List<SubTaskDomain> = emptyList(),
    val typeTask : String,
    val priority: String = "Media",
    val isActive: Boolean = true,
    val isDone: Boolean = false,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val categoryIcon: String = "ic_label", // Icono por defecto
    val categoryColor: String = "category_default",

    // --- Campos Mapeados 1:1 con Google Calendar ---

    // REEMPLAZA A: title
    val summary: String,

    // REEMPLAZA A: description
    val description: String? = null,

    // REEMPLAZA A: place
    val location: String? = null,

    // REEMPLAZA A: calendarColorId
    val colorId: String? = null,

    // REEMPLAZA A: date, hour
    val start: GoogleEventDateTime?,

    // REEMPLAZA A: duration (la duración ahora se calcula: end - start)
    val end: GoogleEventDateTime?,

    // REEMPLAZA A: attendees: List<String>
    val attendees: List<GoogleEventAttendee> = emptyList(),

    // REEMPLAZA A: recurrenceRule: String?
    val recurrence: List<String> = emptyList(),

    // REEMPLAZA A: reminders: List<Int>
    val reminders: GoogleEventReminders = GoogleEventReminders(useDefault = false),

    // Se mantiene (mapeo 1:1)
    val transparency: String = "opaque",

    // Se mantiene (campo simplificado para la lógica de Synkrón)
    val conferenceLink: String? = null

) : Parcelable