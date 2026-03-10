package com.manuelbena.synkron.domain.models


data class Budget(
    val id: String,
    val categoryName: String,
    val limit: Double,
    val spent: Double,
    val colorHex: String
) {
    val remaining: Double get() = limit - spent
    val percentUsed: Int get() = if (limit > 0) ((spent / limit) * 100).toInt() else 0
}