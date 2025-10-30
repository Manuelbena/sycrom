package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.domain.models.TaskDomain

fun TaskDao.toDomain() =
    TaskDomain(
        hour = hour,
        date = date,
        title = title,
        description = "",
        typeTask = typeTask,
        place = "",
        isActive = isActive,
        isDone = isDone,
        subTasks = listOf(),
        duration = 0

    )