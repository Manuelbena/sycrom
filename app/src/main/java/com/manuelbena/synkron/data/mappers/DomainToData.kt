package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.domain.models.TaskDomain

fun TaskDomain.toData() =
    TaskDao(
        id = 0,
        title = title,
        description = description,
        date = date,
        hour = hour,
        typeTask = typeTask,
        isActive = isActive,
        isDone = isDone,
        location = place

    )