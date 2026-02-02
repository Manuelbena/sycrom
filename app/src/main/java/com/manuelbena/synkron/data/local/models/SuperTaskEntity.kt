package com.manuelbena.synkron.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manuelbena.synkron.domain.models.SubTaskItem
import com.manuelbena.synkron.domain.models.SuperTaskModel
import com.manuelbena.synkron.domain.models.SuperTaskType

@Entity(tableName = "super_tasks")
data class SuperTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // Guardaremos el inicio del día en millis
    val title: String,
    val description: String?,
    val type: SuperTaskType,
    val subTasks: List<SubTaskItem>
)

// Mapper: Entity -> Domain
fun SuperTaskEntity.toDomain(): SuperTaskModel {
    return SuperTaskModel(
        id = id,
        date = date,
        title = title,
        description = description,
        type = type,
        subTasks = subTasks,
        completedCount = subTasks.count { it.isCompleted },
        totalCount = subTasks.size
    )
}

// Mapper: Domain -> Entity
fun SuperTaskModel.toEntity(): SuperTaskEntity {
    return SuperTaskEntity(
        id = id,
        date = date,
        title = title,
        description = description,
        type = type,
        subTasks = subTasks
    )
}