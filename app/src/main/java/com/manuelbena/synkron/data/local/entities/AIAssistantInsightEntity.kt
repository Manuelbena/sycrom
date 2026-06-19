package com.manuelbena.synkron.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_assistant_insights")
data class AIAssistantInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String,
    val type: String, // "AUDITORIA" or "FOCO"
    val date: Long
)
