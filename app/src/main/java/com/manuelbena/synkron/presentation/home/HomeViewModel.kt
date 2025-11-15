package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.DeleteTaskUseCase
import com.manuelbena.synkron.domain.usecase.GetTaskTodayUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay // ⬅️ NUEVO IMPORT
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flow // ⬅️ NUEVO IMPORT
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTaskTodayUseCase: GetTaskTodayUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,

    ) : ViewModel() {

    // --- ESTADO (StateFlow) ---
    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    // --- ACCIÓN (SingleLiveEvent) ---
    private val _action = SingleLiveEvent<HomeAction>()
    val action: SingleLiveEvent<HomeAction> = _action

    private val headerDateFormatter = DateTimeFormatter
        .ofPattern("EEEE, dd 'de' MMMM", Locale("es", "ES"))


    init {
        // --- ¡PROGRAMACIÓN REACTIVA! ---
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged()
                .flatMapLatest { date ->
                    _uiState.update { it.copy(isLoading = true) } // 1. Pone isLoading = true

                    // ⬇️ INICIO DEL CAMBIO: Flow con delay ⬇️
                    // 2. Creamos un flow que espera 300ms
                    flow {
                        delay(300L)
                        // 3. Tras esperar, nos suscribimos al caso de uso
                        getTaskTodayUseCase(date).collect { tasks ->
                            emit(tasks) // 4. Emitimos las tareas
                        }
                    }
                    // ⬆️ FIN DEL CAMBIO ⬆️
                }
                .catch { e ->
                    _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al cargar tareas"))
                }
                .collect { tasks ->
                    // 5. Este 'collect' solo se ejecuta después del delay + la
                    //    llegada de datos, poniendo isLoading = false
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = tasks,
                            headerText = it.selectedDate.format(headerDateFormatter).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            }
                        )
                    }
                }
        }
    }

    /**
     * Llamado por la UI cuando el usuario selecciona una nueva fecha.
     */
    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    /**
     * Llamado por la UI para refrescar el calendario a "Hoy".
     */
    fun refreshToToday() {
        onDateSelected(LocalDate.now())
    }

    /**
     * Llamado cuando el usuario (des)marca una tarea.
     */
    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task.copy(isDone = isDone))
            } catch (e: Exception) {
                _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al actualizar"))
            }
        }
    }

    /**
     * Llamado desde el menú de la tarjeta de tarea.
     */
    fun onTaskMenuAction(action: TaskAdapter.TaskMenuAction) {
        viewModelScope.launch {
            when (action) {
                is TaskAdapter.TaskMenuAction.OnDelete -> {
                    try {

                        _action.postValue(HomeAction.ShowErrorSnackbar("Tarea eliminada"))
                    } catch (e: Exception) {
                        _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al eliminar"))
                    }
                }
                is TaskAdapter.TaskMenuAction.OnEdit -> {
                    _action.postValue(HomeAction.NavigateToEditTask(action.task))
                }
                is TaskAdapter.TaskMenuAction.OnShare -> {
                    _action.postValue(HomeAction.ShareTask(action.task))
                }
            }
        }
    }

    // Clase sellada para las Acciones (Eventos de un solo uso)
    sealed class HomeAction {
        data class NavigateToEditTask(val task: TaskDomain) : HomeAction()
        data class ShowErrorSnackbar(val message: String) : HomeAction()
        data class ShareTask(val task: TaskDomain) : HomeAction()
    }
}