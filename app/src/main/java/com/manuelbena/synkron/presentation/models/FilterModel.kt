package com.manuelbena.synkron.presentation.models

data class FilterModel(
    val id: Int,
    val name: String,
    val count: Int,
    var isSelected: Boolean = false,
    val iconRes: Int? = null // El icono es opcional
)