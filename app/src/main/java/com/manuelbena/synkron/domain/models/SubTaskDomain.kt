package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Este es el nuevo modelo canónico para SubTask.
 * Reemplaza a SubTaskPresentation y vive en el dominio
 * para que tanto Data como Presentation puedan acceder a él.
 */
@Parcelize
data class SubTaskDomain(
    val id: String = UUID.randomUUID().toString(),
    val title : String,
    val isDone : Boolean,
): Parcelable
