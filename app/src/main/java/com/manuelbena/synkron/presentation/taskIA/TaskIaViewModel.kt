package com.manuelbena.synkron.presentation.taskIA

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.models.TaskDomain // <-- Importante
import com.manuelbena.synkron.domain.usecase.SendTaskIaUseCase
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskIaViewModel @Inject constructor(
    private val sendTaskIaUseCase: SendTaskIaUseCase
) : ViewModel() {

    private val _iaResponseState = SingleLiveEvent<IaUiState>()
    val iaResponseState: SingleLiveEvent<IaUiState> get() = _iaResponseState

    fun sendMessageToIa(userMessage: String) {
        _iaResponseState.value = IaUiState.Loading

        viewModelScope.launch {
            // El UseCase ya nos devuelve un Result<TaskDomain> gracias a los cambios anteriores
            val result = sendTaskIaUseCase(userMessage)

            result.onSuccess { taskDomain ->
                // ✅ CORREGIDO: Guardamos la 'task' en el estado Success
                _iaResponseState.value = IaUiState.Success(taskDomain)

            }.onFailure { error ->
                _iaResponseState.value = IaUiState.Error(error.message ?: "Error desconocido")
            }
        }
    }

    // Sealed class actualizada
    sealed class IaUiState {
        object Loading : IaUiState()

        // ✅ CAMBIO CLAVE: Usamos 'task' y 'TaskDomain' para que coincida con el BottomSheet
        data class Success(val task: TaskDomain) : IaUiState()

        data class Error(val message: String) : IaUiState()
    }
}