package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.GetTaskTodayUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel // 1. Hilt sabe cómo crear este ViewModel
class HomeViewModel @Inject constructor(
    private val getTaskToday : GetTaskTodayUseCase // 2. Hilt inyecta la dependencia
) : BaseViewModel<HomeEvent>() { // 3. Heredamos y especificamos el tipo de evento

    init {
        getTaskToday()
    }
    fun getTaskToday(){
        // AHORA:
        // Usamos el helper executeFlow de tu BaseViewModel
        executeFlow(
            useCase = { getTaskToday.invoke() }, // Esto devuelve el Flow
            onEach = { taskList ->
                // "onEach" se ejecutará CADA VEZ que la lista en la BD cambie
                _event.value = HomeEvent.ListTasksToday(taskList)
            },
            onError = {
                // Opcional: manejar errores
                _event.value = HomeEvent.ShowErrorSnackbar("Error al cargar tareas")
            }
        )
    }

    fun onTaskMenuAction(action: TaskAdapter.TaskMenuAction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (action) {
                    is TaskAdapter.TaskMenuAction.OnDelete -> {
                        // Llama a tu Caso de Uso para borrar
                        // deleteTaskUseCase(action.task)
                    }
                    is TaskAdapter.TaskMenuAction.OnEdit -> {
                        // Para editar, normalmente navegas a otra pantalla
                        // Aquí podrías emitir un Evento a la UI para que navegue
                        // _events.emit(HomeEvent.NavigateToEditTask(action.task.id))
                    }
                    is TaskAdapter.TaskMenuAction.OnShare -> {
                        // La lógica de compartir (Intent.ACTION_SEND)
                        // se maneja mejor en el Fragment,
                        // así que puedes emitir otro evento.
                        // _events.emit(HomeEvent.ShareTask(action.task))
                    }
                }
            } catch (e: Exception) {
                // Manejar errores
            }
        }
    }

    // El resto de la lógica permanece igual...
    private fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return LocalDate.now().format(formatter)
    }

    fun onDateSelected(date: LocalDate) {
        // Lógica para cargar datos de una nueva fecha
    }

    fun onTaskClicked(task: TaskDomain) {
        // Ejemplo de cómo usar el evento para navegar
        // _event.value = HomeEvent.NavigateToTaskDetail(task.id)
    }
}