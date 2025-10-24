package com.manuelbena.synkron.presentation.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TaskPresentation(
    val hour: String,
    val date : Int,
    val title: String,
    val description: String,
    val typeTask : String,
    val place: String,
    val subTasks: List<SubTaskPresentation>,
    val isActive: Boolean,
    val isDone: Boolean
) : Parcelable
