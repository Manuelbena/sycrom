package com.manuelbena.synkron.data.repository


import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.presentation.models.SubTaskPresentation
import com.manuelbena.synkron.presentation.models.TaskPresentation
import javax.inject.Inject


class TasksRepository @Inject constructor(

) : ITaskRepository {

    override suspend fun getTaskToday(): List<TaskPresentation> {
        return listOf(
            TaskPresentation(
                hour = "10:00 AM",
                date = 20251008, // Formato AAAA MM DD
                title = "Sincronización Semanal del Proyecto",
                description = "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas.Revisar los avances del sprint actual y planificar las próximas tareas.Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "" +
                        "vRevisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "Revisar los avances del sprint actual y planificar las próximas tareas." +
                        "",
                typeTask = "Trabajo",
                place = "Oficina, Sala de Juntas 3",
                subTasks = listOf(
                    SubTaskPresentation(title = "Preparar métricas de rendimiento", isDone = true),
                    SubTaskPresentation(title = "Revisar bloqueos del equipo", isDone = false),
                    SubTaskPresentation(title = "Definir próximos pasos", isDone = false)
                ),
                isActive = true, // La tarea está en curso o es la siguiente
                isDone = false   // Aún no ha sido completada
            ),
            TaskPresentation(
                hour = "17:30",
                date = 20251010,
                title = "Cita con el Dentista",
                description = "Revisión y limpieza anual.",
                typeTask = "Salud",
                place = "Clínica Dental 'Sonrisa Sana'",
                subTasks = emptyList(), // No tiene subtareas
                isActive = false,
                isDone = false
            ),
            TaskPresentation(
                hour = "16:00",
                date = 20251006, // Una fecha pasada
                title = "Estudiar para el examen de Álgebra",
                description = "Repasar los capítulos 4 y 5.",
                typeTask = "Estudio",
                place = "Biblioteca Central",
                subTasks = listOf(
                    SubTaskPresentation(title = "Resumir capítulo 4", isDone = true),
                    SubTaskPresentation(title = "Hacer ejercicios del capítulo 5", isDone = true)
                ),
                isActive = false, // Ya no está activa porque pasó la fecha
                isDone = true     // La marcamos como completada
            )
        )

    }


}