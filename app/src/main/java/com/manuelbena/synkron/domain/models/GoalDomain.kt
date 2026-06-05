package com.manuelbena.synkron.domain.models

data class GoalDomain(
    val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val colorHex: String,
    val deadline: Long? = null
) {
    val progressPercent: Int get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt() else 0
}