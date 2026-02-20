package com.manuelbena.synkron.presentation.models

data class FilterModel(
    val id: Int, // ID único para identificar qué vista mostrar
    val name: String,
    val count: Int = 0, // Opcional, para el badge
    val iconRes: Int? = null, // Opcional
    var isSelected: Boolean = false
)