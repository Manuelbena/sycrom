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

    // --- ESTADO DE DATOS (Variables temporales) ---
    // Estas variables almacenan la selección del usuario mientras crea la tarea
    var iconNameStr: String = ""
    var colorNameStr: String = ""

    // --- ESTADO (State) ---
    private val _state = MutableLiveData<TaskContract.TaskState>(TaskContract.TaskState.Idle)
    val state: LiveData<TaskContract.TaskState> = _state

    // --- ACCIÓN (Action) ---
    private val _action = SingleLiveEvent<TaskContract.TaskAction>()
    val action: LiveData<TaskContract.TaskAction> = _action

    // --- ESTADO DE RECURRENCIA ---
    private val _recurrenceState = MutableLiveData<RecurrenceType>(RecurrenceType.NONE)
    val recurrenceState: LiveData<RecurrenceType> get() = _recurrenceState

    // Función para actualizar la recurrencia desde la UI
    fun setRecurrence(type: RecurrenceType) {
        _recurrenceState.value = type
    }

    // Función para recibir la categoría seleccionada desde el Diálogo
    fun updateCategorySelection(icon: String, color: String) {
        this.iconNameStr = icon
        this.colorNameStr = color
    }

    /**
     * Manejador central de eventos provenientes de la UI.
     */
     fun onEvent(event: TaskContract.TaskEvent) { // Asegúrate de usar 'override' si BaseViewModel lo requiere
        when (event) {
            is TaskContract.TaskEvent.OnSaveTask -> onSaveTask(event.task)
            // Aquí podrías añadir más eventos, ej: OnCategoryClicked
        }
    }

    /**
     * Inicia el proceso de guardado.
     * AQUÍ OCURRE LA MAGIA: Fusionamos los datos del Fragment con los del ViewModel.
     */
    private fun onSaveTask(taskFromUI: TaskDomain) {
        viewModelScope.launch {
            _state.value = TaskContract.TaskState.Loading
            try {
                // 1. ENRIQUECER LA TAREA:
                // Copiamos la tarea que viene de la UI y le sobreescribimos
                // la categoría y la recurrencia con lo que tenemos en el ViewModel.
                val taskToSave = taskFromUI.copy(
                    categoryIcon = iconNameStr,
                    categoryColor = colorNameStr,
                    synkronRecurrence = _recurrenceState.value ?: RecurrenceType.NONE,
                    // NOTA: synkronRecurrenceDays debe venir lleno desde 'taskFromUI' (el Fragment)
                    // ya que el ViewModel no controla directamente los Chips.
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