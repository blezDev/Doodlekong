package com.blez.doodlekong.ui.drawing

import androidx.lifecycle.ViewModel
import com.blez.doodlekong.R
import com.blez.doodlekong.utils.DispatcherProvider
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(private val dispatchers : DispatcherProvider, val gson : Gson ) : ViewModel() {
    private val _selectedColorButtonId = MutableStateFlow(R.id.rbBlack)
   val selectedColorButtonId : StateFlow<Int>
   get() = _selectedColorButtonId

    fun checkRadioButton(id : Int)
    {
        _selectedColorButtonId.value = id
    }
}