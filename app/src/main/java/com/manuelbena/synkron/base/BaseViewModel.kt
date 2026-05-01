package com.manuelbena.synkron.base

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing common logic for use case execution and event handling.
 */
abstract class BaseViewModel<E> : ViewModel() {

    protected val _event = SingleLiveEvent<E>()
    val event: LiveData<E> get() = _event

    protected val tag: String = this::class.java.simpleName

    /**
     * Executes a suspended use case.
     */
    protected fun <R> executeUseCase(
        useCase: suspend () -> R,
        onSuccess: (R) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Job {
        return viewModelScope.launch {
            try {
                val result = useCase()
                onSuccess(result)
            } catch (e: Throwable) {
                Log.e(tag, "Error in executeUseCase: ${e.message}", e)
                onError(e)
            }
        }
    }

    /**
     * Executes a flow-based use case and collects its values.
     */
    protected fun <R> executeFlow(
        useCase: () -> Flow<R>,
        onEach: (R) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): Job {
        return viewModelScope.launch {
            useCase()
                .catch { e ->
                    Log.e(tag, "Error in executeFlow: ${e.message}", e)
                    onError(e)
                }
                .collect { data ->
                    onEach(data)
                }
        }
    }
}
