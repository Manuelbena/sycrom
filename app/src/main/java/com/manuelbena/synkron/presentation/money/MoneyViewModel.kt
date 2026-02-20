package com.manuelbena.synkron.presentation.money


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoneyViewModel @Inject constructor() : ViewModel() {

    // 0 = Resumen, 1 = Movimientos
    private val _selectedTab = MutableLiveData<Int>(0)
    val selectedTab: LiveData<Int> = _selectedTab

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
}