package com.blez.doodlekong.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blez.doodlekong.data.remote.ws.Room
import com.blez.doodlekong.ui.drawing.repository.SetupRepository
import com.blez.doodlekong.utils.Constants.MAX_ROOM_NAME_LENGTH
import com.blez.doodlekong.utils.Constants.MAX_USERNAME_LENGTH
import com.blez.doodlekong.utils.Constants.MIN_ROOM_NAME_LENGTH
import com.blez.doodlekong.utils.Constants.MIN_USERNAME_LENGTH
import com.blez.doodlekong.utils.DispatcherProvider
import com.blez.doodlekong.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectRoomViewModel @Inject constructor(private val setupRepository: SetupRepository, private val dispatcherState: DispatcherProvider) : ViewModel() {

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


    data class JoinRoomEvent(val roomName : String) : SetupEvent()
    data class JoinRoomErrorEvent(val error : String) : SetupEvent()
}
    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent : SharedFlow<SetupEvent>
    get() = _setupEvent


    private val _rooms = MutableStateFlow<SetupEvent>(SetupEvent.GetRoomEmptyEvent)
    val rooms : StateFlow<SetupEvent> = _rooms




    fun getRooms(searchQuery : String)
    {
        _rooms.value = SetupEvent.GetRoomLoadingEvent
        viewModelScope.launch(dispatcherState.main) {
        val result = setupRepository.getRoom(searchQuery)
            if (result is Resource.Success)
                _rooms.value = SetupEvent.GetRoomEvent(result.data?: return@launch)
            else
            _setupEvent.emit(SetupEvent.GetRoomErrorEvent(result.message ?: return@launch))

        }
    }

    fun joinRoom(username : String,roomName : String)
    {
        _rooms.value = SetupEvent.GetRoomLoadingEvent
        viewModelScope.launch(dispatcherState.main) {
            val result = setupRepository.joinRoom(username, roomName)
            if (result is Resource.Success)
                _setupEvent.emit(SetupEvent.JoinRoomEvent(roomName))
            else
                _setupEvent.emit(SetupEvent.JoinRoomErrorEvent(result.message ?: return@launch))

        }
    }

}