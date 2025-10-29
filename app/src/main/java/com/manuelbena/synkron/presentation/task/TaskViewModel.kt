package com.manuelbena.synkron.presentation.task

import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel // <-- 1. IMPORTA LA ANOTACIÓN
import javax.inject.Inject // <-- 2. IMPORTA @Inject

@HiltViewModel // <-- 3. ANOTA LA CLASE
class TaskViewModel @Inject constructor( // <-- 4. ANOTA EL CONSTRUCTOR
    private val insertTask: InsertNewTaskUseCase
) : BaseViewModel<TaskEvent>() {

    fun onSaveTask(task: TaskDomain) {


        executeUseCase(
            useCase = { insertTask(task) },
            onSuccess = {
                // El caso de uso se ejecutó correctamente
                _event.value = TaskEvent.InsertTask
            },
            onError = { throwable ->
                // Ocurrió un error, lo puedes propagar a la UI
                _event.value = TaskEvent.ErrorSavingTask(throwable.message ?: "Error desconocido")
            }
        )
    }


}
