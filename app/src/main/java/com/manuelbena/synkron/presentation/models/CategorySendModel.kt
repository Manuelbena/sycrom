package com.manuelbena.synkron.presentation.models

data class CategorySendModel(
    val id: Int,
    val name: String,
    val amount: String, // Ej: "380,00 €"
    val percentage: String, // Ej: "98.7% del total"
    val progress: Int, // 0 a 100
    val iconRes: Int, // R.drawable.ic_pizza
    val colorHex: String // Color para el icono y la barra (Ej: "#F57C00")
)