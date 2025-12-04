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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTaskTodayUseCase: GetTaskTodayUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _action = SingleLiveEvent<HomeAction>()
    val action: SingleLiveEvent<HomeAction> = _action

    // 🔥 NUEVO: Gatillo para forzar la recarga manual
    private val _refreshTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val headerDateFormatter = DateTimeFormatter
        .ofPattern("EEEE, dd 'de' MMMM", Locale("es", "ES"))

    init {
        // Inicializamos fecha hoy
        _uiState.update { it.copy(selectedDate = LocalDate.now()) }

        viewModelScope.launch {
            // 🔥 MERGE: Escuchamos cambios de fecha O el gatillo de refresco
            merge(
                _uiState.map { it.selectedDate }.distinctUntilChanged(), // 1. Si cambia la fecha
                _refreshTrigger.map { _uiState.value.selectedDate }      // 2. Si forzamos refresh (usa la fecha actual)
            )
                .flatMapLatest { date ->
                    _uiState.update { it.copy(isLoading = true) }

                    flow {
                        delay(300L) // Mantenemos tu delay suave
                        getTaskTodayUseCase(date).collect { tasks ->
                            emit(tasks)
                        }
                    }
                }
                .catch { e ->
                    _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al cargar tareas"))
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { tasks ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = tasks,
                            headerText = it.selectedDate.format(headerDateFormatter).replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                            }
                        )
                    }
                }
        }
    }

    /**
     * 🔥 NUEVA FUNCIÓN: Llama a esto desde onResume()
     */
    fun refreshData() {
        _refreshTrigger.tryEmit(Unit)
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun refreshToToday() {
        val today = LocalDate.now()
        // Si ya estamos en hoy, forzamos el refresh manual, si no, cambiamos la fecha
        if (_uiState.value.selectedDate == today) {
            refreshData()
        } else {
            onDateSelected(today)
        }
    }

    // ... (El resto de funciones: onTaskCheckedChanged, onTaskMenuAction se mantienen igual) ...
    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task.copy(isDone = isDone))
            } catch (e: Exception) {
                _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al actualizar"))
            }
        }
    }

    fun onTaskMenuAction(action: TaskAdapter.TaskMenuAction) {
        viewModelScope.launch {
            when (action) {
                is TaskAdapter.TaskMenuAction.OnDelete -> {
                    try {
                        deleteTaskUseCase(action.task)
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
}