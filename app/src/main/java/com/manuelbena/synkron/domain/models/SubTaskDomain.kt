package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Este es el nuevo modelo canónico para SubTask.
 * Reemplaza a SubTaskPresentation y vive en el dominio
 * para que tanto Data como Presentation puedan acceder a él.
 */
@Parcelize
data class SubTaskDomain(
    val title : String,
    val isDone : Boolean,
): Parcelable
