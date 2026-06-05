package com.manuelbena.synkron.domain.models

data class GoalContributionDomain(
    val id: Int = 0,
    val goalId: Int,
    val amount: Double,
    val dateMillis: Long
)