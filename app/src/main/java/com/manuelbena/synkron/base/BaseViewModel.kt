package com.manuelbena.synkron.base

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * ViewModel base con lógica reutilizable para ejecutar casos de uso y manejar el estado.
 */
abstract class BaseViewModel<E> : ViewModel() {

    protected open val _event by lazy { MutableLiveData<E>() }
    open val event: LiveData<E> = _event

    protected val tag: String = this::class.java.simpleName

    /**
     * Ejecuta un caso de uso suspendido que devuelve un único resultado.
     * Ideal para operaciones como inserciones, actualizaciones o borrados.
     *
     * @param R Tipo de retorno del caso de uso.
     * @param useCase La función suspendida a ejecutar.
     * @param onSuccess Callback en caso de éxito.
     * @param onError Callback en caso de error.
     */
    protected fun <R> executeUseCase(
        useCase: suspend () -> R,
        onSuccess: (R) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val result = useCase()
                onSuccess(result)
            } catch (e: Throwable) {
                Log.e(tag, "Error en executeUseCase: ${e.message}", e)
                onError(e)
            }
        }
    }

    /**
     * Ejecuta un caso de uso que devuelve un Flow y lo recolecta.
     * Ideal para observar cambios en la base de datos o streams de datos.
     *
     * @param R Tipo de dato emitido por el Flow.
     * @param useCase El caso de uso que devuelve un Flow<R>.
     * @param onEach Callback que se ejecuta para cada valor emitido por el Flow.
     * @param onCompletion Callback opcional que se ejecuta cuando el Flow termina (raro en Room).
     * @param onError Callback en caso de un error durante la recolección.
     */
    protected fun <R> executeFlow(
        useCase: () -> Flow<R>,
        onEach: (R) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            useCase()
                .catch { e ->
                    Log.e(tag, "Error en executeFlow: ${e.message}", e)
                    onError(e)
                }
                .collect { data ->
                    onEach(data)
                }
        }
    }
}
