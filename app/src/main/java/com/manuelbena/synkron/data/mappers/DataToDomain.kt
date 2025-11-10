package com.manuelbena.synkron.data.mappers

import android.R.attr.priority
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.domain.models.TaskDomain

fun TaskDao.toDomain() =
    TaskDomain(
        id = this.id,
        hour = hour,
        date = date,
        title = title,
        description = description ?: "",
        typeTask = typeTask,
        place = location ?: "",
        isActive = isActive,
        isDone = isDone,
        subTasks = subTasks,
        duration = duration,
        isDeleted = isDeleted,
        isArchived = isArchived,
        isPinned = isPinned,
        priority = priority
    )
