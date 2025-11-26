package com.manuelbena.synkron.presentation.task

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.RecurrenceType
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val insertTask: InsertNewTaskUseCase
) : BaseViewModel<TaskContract.TaskEvent>() {

    // --- ELIMINADO: Estas variables ya no hacen falta y eran la causa del error ---
    // var iconNameStr: String = ""
    // var colorNameStr: String = ""

    // --- ESTADO (State) ---
    private val _state = MutableLiveData<TaskContract.TaskState>(TaskContract.TaskState.Idle)
    val state: LiveData<TaskContract.TaskState> = _state

    // --- ACCIÓN (Action) ---
    private val _action = SingleLiveEvent<TaskContract.TaskAction>()
    val action: LiveData<TaskContract.TaskAction> = _action

    // --- ESTADO DE RECURRENCIA ---
    private val _recurrenceState = MutableLiveData<RecurrenceType>(RecurrenceType.NOTIFICATION)
    val recurrenceState: LiveData<RecurrenceType> get() = _recurrenceState

    fun setRecurrence(type: RecurrenceType) {
        _recurrenceState.value = type
    }

    // --- ELIMINADO: Ya no la usamos, el Fragment gestiona esto ---
    // fun updateCategorySelection(icon: String, color: String) { ... }

     fun onEvent(event: TaskContract.TaskEvent) {
        when (event) {
            is TaskContract.TaskEvent.OnSaveTask -> onSaveTask(event.task)
        }
    }

    /**
     * Inicia el proceso de guardado.
     * CORRECCIÓN: Respetamos el categoryIcon y categoryColor que vienen del Fragment.
     * Solo inyectamos la recurrencia porque esa sí vive en el ViewModel.
     */
    private fun onSaveTask(taskFromUI: TaskDomain) {
        viewModelScope.launch {
            _state.value = TaskContract.TaskState.Loading
            try {
                // 1. ENRIQUECER LA TAREA:
                // Solo sobrescribimos la recurrencia (que controla el VM).
                // EL RESTO (Icono, Color, Título) SE RESPETA TAL CUAL VIENE DE LA UI.
                val taskToSave = taskFromUI.copy(
                    synkronRecurrence = _recurrenceState.value ?: RecurrenceType.NOTIFICATION,
                    // Nota: synkronRecurrenceDays ya viene lleno del Fragment
                )

                // 2. Insertar en base de datos
                insertTask(taskToSave)

                _state.value = TaskContract.TaskState.Success("¡Tarea guardada!")
                _action.value = TaskContract.TaskAction.NavigateBack
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Error desconocido al guardar"
                _state.value = TaskContract.TaskState.Error(errorMsg)
                _action.value = TaskContract.TaskAction.ShowErrorSnackbar(errorMsg)
            }
        }
    }
}