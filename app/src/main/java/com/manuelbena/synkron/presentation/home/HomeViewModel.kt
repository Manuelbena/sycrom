package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.GetTasksForDateUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTaskTodayUseCase: GetTasksForDateUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) : BaseViewModel<HomeEvent>() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    override val _event = MutableLiveData<HomeEvent>()
    override val event: LiveData<HomeEvent> = _event

    private val headerDateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    private val selectedDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale("es", "ES"))


    init {
        // Carga los datos para el día de hoy al iniciar
        val today = LocalDate.now()
        _selectedDate.value = today // Setea la fecha inicial
        getHeaderDate(today) // Actualiza el texto del header
        updateSelectedDateHeader(today) // Actualiza el texto de la fecha seleccionada

        // --- INICIO DE CAMBIOS (LÓGICA DE RECOLECCIÓN DE FLOW) ---

        // Lanzamos UNA corrutina que vivirá mientras viva el ViewModel.
        viewModelScope.launch {
            // Se suscribe a CUALQUIER cambio en _selectedDate
            _selectedDate.flatMapLatest { date ->
                // flatMapLatest cancela el Flow anterior (del día viejo)
                // y se suscribe al Flow nuevo (del día nuevo).
                getTaskTodayUseCase(date) // Esto devuelve Flow<List<TaskDomain>>
            }.catch { e ->
                // Si el Flow de la base de datos falla, lo capturamos aquí
                _event.value = HomeEvent.ShowErrorSnackbar("Error al cargar las tareas: ${e.message}")
            }.collect { tasks ->
                // Cada vez que la lista de tareas para 'date' cambie en Room,
                // esto se ejecutará y enviará la nueva lista a la UI.
                _event.value = HomeEvent.ListTasksToday(tasks)
            }
        }
        // --- FIN DE CAMBIOS ---
    }

    /**
     * Se llama cuando el usuario selecciona una fecha en el calendario.
     */
    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date // Actualiza la fecha seleccionada
        updateSelectedDateHeader(date)
    }

    /**
     * Esta es la nueva función clave.
     * Fuerza al ViewModel a refrescarse al día de HOY.
     * Usado al resumir la app y a medianoche.
     */
    fun refreshToToday() {
        val today = LocalDate.now()
        _selectedDate.value = today
        getHeaderDate(today)
        updateSelectedDateHeader(today)

        // Emite el evento para que el Fragment redibuje el calendario
        _event.value = HomeEvent.RefreshCalendarUI
    }


    private fun getHeaderDate(date: LocalDate) {
        val formattedDate = date.format(headerDateFormatter).replaceFirstChar { it.uppercase() }
        _event.value = HomeEvent.UpdateHeaderText(formattedDate)
    }

    private fun updateSelectedDateHeader(date: LocalDate) {
        val formattedDate = date.format(selectedDateFormatter).replaceFirstChar { it.uppercase() }
        _event.value = HomeEvent.UpdateSelectedDate(formattedDate)
    }

    // --- FIN DE CAMBIOS ---

    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task.copy(isDone = isDone))
                // Refresca la lista de tareas después de actualizar

            } catch (e: Exception) {
                _event.value = HomeEvent.ShowErrorSnackbar("Error al actualizar la tarea")
            }
        }
    }
    fun onSubtaskCheckedChanged(task: TaskDomain, subtaskToUpdate: SubTaskDomain, isDone: Boolean) {
        viewModelScope.launch {
            try {
                // Creamos una nueva lista de subtareas
                val newSubtasks = task.subTasks.map { subtask ->

                    if (subtask.id == subtaskToUpdate.id) {
                        subtask.copy(isDone = isDone)
                    } else {
                        subtask
                    }
                }

                // Creamos la nueva tarea actualizada con la lista de subtareas
                val updatedTask = task.copy(subTasks = newSubtasks)

                // Llamamos al caso de uso para guardar la tarea (padre) actualizada
                updateTaskUseCase(updatedTask)


                // 2. Emitimos un evento para que el BottomSheet (detalle) se actualice
                _event.value = HomeEvent.TaskUpdated(updatedTask)

            } catch (e: Exception) {
                _event.value = HomeEvent.ShowErrorSnackbar("Error al actualizar la subtarea")
            }
        }
    }
    // --- FIN DE CAMBIOS ---

    fun onTaskMenuAction(action: TaskAdapter.TaskMenuAction) {
        when (action) {
            is TaskAdapter.TaskMenuAction.OnDelete -> {
                // Lógica de borrado (pendiente)
                _event.value = HomeEvent.ShowErrorSnackbar("Borrar tarea (pendiente)")
            }
            is TaskAdapter.TaskMenuAction.OnEdit -> {
                _event.value = HomeEvent.NavigateToEditTask(action.task)
            }
            is TaskAdapter.TaskMenuAction.OnShare -> {
                _event.value = HomeEvent.ShareTask(action.task)
            }
        }
    }
}