package com.manuelbena.synkron.domain.models



import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SuperTaskModel(
    val id: Int,
    val title: String,
    val iconRes: Int,
    val completedCount: Int,
    val totalCount: Int,
    val subTasks: List<SubTaskItem>
) : Parcelable

@Parcelize
data class SubTaskItem(
    val title: String,
    val details: String, // Ej: "4x12 60kg"
    val isCompleted: Boolean
) : Parcelable