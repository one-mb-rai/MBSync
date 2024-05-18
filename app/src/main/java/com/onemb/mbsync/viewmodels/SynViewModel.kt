package com.onemb.mbsync.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SynViewModel: ViewModel() {
    private val _state = MutableStateFlow(true)
    val state: StateFlow<Boolean> get() = _state

    fun updateState(value: Boolean) {
        _state.value = value
    }

}