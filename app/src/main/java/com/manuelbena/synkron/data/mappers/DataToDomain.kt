package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.domain.models.TaskDomain

fun TaskDao.toDomain() =
    TaskDomain(
        hour = hour,
        date = date,
        title = title,
        description = description ?: "", // <-- CORREGIDO (manejar nulo)
        typeTask = typeTask,
        place = location ?: "", // <-- CORREGIDO (manejar nulo)
        isActive = isActive,
        isDone = isDone,
        subTasks = subTasks, // <-- CORREGIDO
        duration = duration // <-- CORREGIDO
    )
