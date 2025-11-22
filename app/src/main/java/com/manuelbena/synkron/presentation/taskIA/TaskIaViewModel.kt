package com.manuelbena.synkron.presentation.taskIA

import androidx.lifecycle.ViewModel

import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskIaViewModel @Inject constructor(

) : ViewModel() {

    val onSuccess = SingleLiveEvent<Unit>()

    fun sendInput(text: String) {

    }
}