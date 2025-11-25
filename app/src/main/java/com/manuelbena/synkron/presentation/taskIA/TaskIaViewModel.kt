package com.manuelbena.synkron.presentation.taskIA

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.usecase.SendTaskIaUseCase
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskIaViewModel @Inject constructor(
    private val sendTaskIaUseCase: SendTaskIaUseCase
) : ViewModel() {

    // LiveData o StateFlow para la UI
    private val _iaResponseState = SingleLiveEvent<IaUiState>()
    val iaResponseState: SingleLiveEvent<IaUiState> get() = _iaResponseState

    fun sendMessageToIa(userMessage: String) {
        _iaResponseState.value = IaUiState.Loading

        viewModelScope.launch {
            val result = sendTaskIaUseCase(userMessage)

            result.onSuccess { n8nResponse ->
                // Aquí procesas la respuesta para la UI
                // Por ejemplo, autocompletar campos en el formulario
                _iaResponseState.value = IaUiState.Success(n8nResponse)

                // Opcional: Si quieres loguear como "Synkrón AI" diría:
                // "He recibido la estructura de la tarea desde el servidor."
            }.onFailure { error ->
                _iaResponseState.value = IaUiState.Error(error.message ?: "Error desconocido")
            }
        }
    }

    // Sealed class para manejar los estados de la vista de forma limpia
    sealed class IaUiState {
        object Loading : IaUiState()
        data class Success(val data: com.manuelbena.synkron.data.remote.n8n.models.N8nChatResponse) : IaUiState()
        data class Error(val message: String) : IaUiState()
    }
}