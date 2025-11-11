package com.manuelbena.synkron.domain.interfaces

import TaskDomain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate // ¡Correcto!

/**
 * Interfaz (Contrato) para el repositorio de Tareas.
 * Define las operaciones que el Dominio (UseCases) puede realizar.
 * Es agnóstico a la fuente de datos (Room, API, etc.)
 */
interface ITaskRepository {

    /**
     * Inserta una nueva tarea.
     */
    suspend fun insertTask(task: TaskDomain)

    /**
     * Actualiza una tarea existente.
     */
    suspend fun updateTask(task: TaskDomain)

    /**
     * Obtiene un Flow (flujo reactivo) de tareas para una fecha específica.
     */
    fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>>

    /**
     * Elimina una tarea.
     */
    suspend fun deleteTask(task: TaskDomain)
}