package com.manuelbena.synkron.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

abstract class BaseViewModel<T>  : ViewModel() {

    protected val _event by lazy { MutableLiveData<T>() }
    val event : LiveData<T> = _event

    protected val tag = this::class.java.name

    fun <T> executeUseCase(
        useCase: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ){
        viewModelScope.launch {
            try {
                useCase().also {
                    result -> onSuccess(result)
                }
            }catch (e: Throwable){
                if (e !is SecurityException)error(e)
            }
        }
    }
}