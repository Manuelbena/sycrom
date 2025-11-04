package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TaskDomain(
    val id: Long = 0L,
    val hour: Int,
    val date : Long,
    val title: String,
    val description: String,
    val duration: Int,
    val typeTask : String,
    val place: String,
    val subTasks: List<SubTaskDomain>,
    val isActive: Boolean,
    val isDone: Boolean
) : Parcelable