package com.manuelbena.synkron.data.local.models


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manuelbena.synkron.domain.models.SubTaskDomain

/**
 * Esta es la entidad (tabla) para tu evento.
 * Representa un evento en la base de datos.
 *
 * TODO: Adapta los campos (propiedades) de esta data class
 * para que coincidan exactamente con los campos de tu pantalla.
 */
@Entity(tableName = "events_table")
data class TaskDao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    var title: String,

    @ColumnInfo(name = "description")
    var description: String?,

    @ColumnInfo(name = "start_time") // 'date' en TaskDomain
    var date: Long,

    @ColumnInfo(name = "hour") // 'hour' en TaskDomain (minutos)
    var hour: Int,

    @ColumnInfo(name = "duration") // <-- AÑADIDO
    var duration: Int,

    @ColumnInfo(name = "tipy_task")
    var typeTask: String,

    @ColumnInfo(name = "is_active")
    var isActive: Boolean = false,

    @ColumnInfo(name = "is_done")
    var isDone: Boolean = false,

    @ColumnInfo(name = "location")
    var location: String?,

    @ColumnInfo(name = "sub_tasks") // <-- AÑADIDO
    var subTasks: List<SubTaskDomain> = emptyList()
)
