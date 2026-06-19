package com.manuelbena.synkron.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.data.local.entities.AIAssistantInsightEntity
import com.manuelbena.synkron.data.local.models.AIAssistantDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val aiAssistantDao: AIAssistantDao
) : ViewModel() {

    val insights: StateFlow<List<AIAssistantInsightEntity>> = aiAssistantDao.getAllInsights()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteInsight(id: Int) {
        viewModelScope.launch {
            aiAssistantDao.deleteInsightById(id)
        }
    }
}
