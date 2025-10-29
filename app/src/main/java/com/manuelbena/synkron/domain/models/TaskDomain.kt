package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import com.manuelbena.synkron.presentation.models.SubTaskPresentation
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TaskDomain(
    val hour: Long,
    val date : Long,
    val title: String,
    val description: String,
    val typeTask : String,
    val place: String,
    val subTasks: List<SubTaskPresentation>,
    val isActive: Boolean,
    val isDone: Boolean
) : Parcelable