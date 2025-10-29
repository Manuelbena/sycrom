package com.manuelbena.synkron.presentation.task


sealed class TaskEvent {
    object InsertTask : TaskEvent()
    object  UpdateTask : TaskEvent()
    object  DeleteTask : TaskEvent()
    class ErrorSavingTask(val message: String) : TaskEvent()
}