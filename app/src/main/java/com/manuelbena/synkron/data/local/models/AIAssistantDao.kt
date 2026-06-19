package com.manuelbena.synkron.data.local.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manuelbena.synkron.data.local.entities.AIAssistantInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIAssistantDao {

    @Query("SELECT * FROM ai_assistant_insights ORDER BY date DESC")
    fun getAllInsights(): Flow<List<AIAssistantInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: AIAssistantInsightEntity)

    @Query("DELETE FROM ai_assistant_insights WHERE id = :id")
    suspend fun deleteInsightById(id: Int)
}
