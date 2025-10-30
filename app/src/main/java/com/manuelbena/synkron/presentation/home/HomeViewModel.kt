package com.manuelbena.synkron.presentation.home

import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.GetTaskTodayUseCase

import dagger.hilt.android.lifecycle.HiltViewModel


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