package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.domain.models.TaskDomain

fun TaskDomain.toData() =
    TaskDao(
        id = 0, // Room generará el ID
        title = title,
        description = description,
        date = date,
        hour = hour,
        duration = duration, // <-- AÑADIDO
        typeTask = typeTask,
        isActive = isActive,
        isDone = isDone,
        location = place,
        subTasks = subTasks // <-- AÑADIDO
    )
