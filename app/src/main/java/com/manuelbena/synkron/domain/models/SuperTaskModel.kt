package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SuperTaskModel(
    val id: Int = 0, // ID autogenerado por Room
    val date: Long,  // Fecha en milisegundos (Epoch) para saber qué día mostrarla
    val title: String,
    val description: String? = null,
    val type: SuperTaskType, // GYM, COOKING, etc.
    val subTasks: List<SubTaskItem>,

    // Calculados (no necesariamente en BD, pero útiles)
    val completedCount: Int = 0,
    val totalCount: Int = 0
) : Parcelable {
    // Helper para obtener el icono según el tipo
    fun getIconRes(): Int {
        // Asumiendo que tienes estos drawables, si no, usa genéricos
        return when (type) {
            SuperTaskType.GYM -> com.manuelbena.synkron.R.drawable.ic_health // Tu icono de pesa/brazo
            SuperTaskType.COOKING -> com.manuelbena.synkron.R.drawable.ic_check_circle // Necesitarás crear este
            SuperTaskType.READING -> com.manuelbena.synkron.R.drawable.ic_book // Necesitarás crear este
            SuperTaskType.MEDITATION -> com.manuelbena.synkron.R.drawable.ic_other
            else -> com.manuelbena.synkron.R.drawable.ic_other
        }
    }
}

@Parcelize
data class SubTaskItem(
    val title: String,   // Ej: "Press Banca" o "Cortar cebolla"
    val details: String, // Ej: "4x12" o "200g"
    val isCompleted: Boolean = false
) : Parcelable