package com.manuelbena.synkron.presentation.home

import com.manuelbena.synkron.base.BaseViewModel // TU CLASE BASE
import com.manuelbena.synkron.data.repository.TasksRepository
import com.manuelbena.synkron.domain.usecase.GetTaskTodayUseCase
import com.manuelbena.synkron.presentation.models.TaskPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel // 1. Hilt sabe cómo crear este ViewModel
class HomeViewModel @Inject constructor(
    private val getTaskToday : GetTaskTodayUseCase // 2. Hilt inyecta la dependencia
) : BaseViewModel<HomeEvent>() { // 3. Heredamos y especificamos el tipo de evento

    fun getTaskToday(){
        executeUseCase({
            getTaskToday.invoke()
        },{
            _event.value = HomeEvent.ListTasksToday(it)

        },{

        })
    }

    // El resto de la lógica permanece igual...
    private fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return LocalDate.now().format(formatter)
    }

    fun onDateSelected(date: LocalDate) {
        // Lógica para cargar datos de una nueva fecha
    }

    fun onTaskClicked(task: TaskPresentation) {
        // Ejemplo de cómo usar el evento para navegar
        // _event.value = HomeEvent.NavigateToTaskDetail(task.id)
    }
}