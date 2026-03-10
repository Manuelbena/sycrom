package com.manuelbena.synkron.presentation.models


data class FilterModel(
    val id: Int,
    val name: String,
    val count: Int = 0,
    val iconRes: Int? = null,
    var isSelected: Boolean = false
)