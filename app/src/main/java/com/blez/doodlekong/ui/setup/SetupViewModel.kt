package com.blez.doodlekong.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blez.doodlekong.data.remote.ws.Room
import com.blez.doodlekong.repository.SetupRepository
import com.blez.doodlekong.utils.Constants.MAX_USERNAME_LENGTH
import com.blez.doodlekong.utils.Constants.MIN_USERNAME_LENGTH
import com.blez.doodlekong.utils.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(private val setupRepository: SetupRepository, private val dispatcherState: DispatcherProvider) : ViewModel() {

sealed class SetupEvent {
    object InputEmpty : SetupEvent()
    object InputEmptyError : SetupEvent()
    object InputTooShortError : SetupEvent()
    object InputTooLongError : SetupEvent()

    data class CreateRoomEvent(val room: Room):SetupEvent()
    data class CreateRoomErrorEvent(val error : String) : SetupEvent()

    data class NavigateToSelectRoomErrorEvent(val error : String) : SetupEvent()
    data class NavigateToSelectRoomEvent(val username : String) : SetupEvent()

    data class GetRoomEvent(val rooms : List<Room>) : SetupEvent()
    data class GetRoomErrorEvent(val error : String) : SetupEvent()
    object GetRoomLoadingEvent : SetupEvent()
    object GetRoomEmptyEvent : SetupEvent()


    data class JoinRoomEvent(val username : String) : SetupEvent()
    data class JoinRoomErrorEvent(val error : String) : SetupEvent()
}
    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent : SharedFlow<SetupEvent>
    get() = _setupEvent


    private val _rooms = MutableStateFlow<SetupEvent>(SetupEvent.GetRoomEmptyEvent)
    val rooms : StateFlow<SetupEvent> = _rooms

    fun validateUsernameAndNavigateToSelectRoom(username : String){
        viewModelScope.launch(dispatcherState.main)
        {
              when {
                  username.isEmpty()->{
                      _setupEvent.emit(SetupEvent.InputEmptyError)
                  }
                  username.length < MIN_USERNAME_LENGTH->{
                      _setupEvent.emit(SetupEvent.InputTooShortError)
                  }
                  username.length > MAX_USERNAME_LENGTH->{
                      _setupEvent.emit(SetupEvent.InputTooLongError)
                  }
                  else ->{
                      _setupEvent.emit(SetupEvent.NavigateToSelectRoomEvent(username))
                  }
              }
        }
    }



}