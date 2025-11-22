package com.manuelbena.synkron.presentation.task_ia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.usecase.SendTaskIaUseCase
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskIaViewModel @Inject constructor(
    private val sendUseCase: SendTaskIaUseCase
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Usamos SingleLiveEvent para eventos de una sola vez (Toast, navegación)
    val onSuccess = SingleLiveEvent<Unit>()
    val onError = SingleLiveEvent<String>()

    fun sendInput(text: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = sendUseCase(text)
            _isLoading.value = false

            result.fold(
                onSuccess = { onSuccess.call() },
                onFailure = { error -> onError.value = error.message ?: "Error desconocido" }
            )
        }
    }
}