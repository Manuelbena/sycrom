package com.manuelbena.synkron.presentation.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubTaskPresentation(
    val title : String,
    val isDone : Boolean,
): Parcelable