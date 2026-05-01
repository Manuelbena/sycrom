package com.manuelbena.synkron.presentation.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Una implementación de [MutableLiveData] que solo emite la actualización de datos
 * una vez (Single Shot).
 *
 * Esto es útil para eventos como la navegación, mostrar un SnackBar o un Toast,
 * que no deben repetirse al re-crearse la vista (ej. por rotación de pantalla).
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {

    /**
     * Usamos un AtomicBoolean para asegurar que el evento solo se consuma una vez,
     * incluso en escenarios de concurrencia.
     */
    private val pending = AtomicBoolean(false)

    /**
     * Observa el LiveData.
     *
     * @param owner El [LifecycleOwner] que controla al observador.
     * @param observer El [Observer] que recibirá el dato.
     */
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) { t ->
            // Comparamos y establecemos: si 'pending' es true, lo pone a false y entra.
            // Si era false, no entra.
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }

    /**
     * Establece el valor del LiveData y marca el evento como pendiente.
     *
     * @param t El nuevo valor.
     */
    override fun setValue(t: T?) {
        pending.set(true) // Marcamos que hay un evento pendiente
        super.setValue(t)
    }

    /**
     * Una variante de [setValue] que es útil para eventos sin datos (ej. navegar atrás).
     * Simplemente dispara la notificación.
     */
    fun call() {
        value = null
    }
}