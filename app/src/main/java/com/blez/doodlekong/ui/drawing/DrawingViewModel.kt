package com.blez.doodlekong.ui.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blez.doodlekong.R
import com.blez.doodlekong.data.remote.ws.*
import com.blez.doodlekong.data.remote.ws.DrawAction.Companion.ACTION_UNDO
import com.blez.doodlekong.utils.DispatcherProvider
import com.google.gson.Gson
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    val gson: Gson,
    val drawingApi: DrawingApi
) : ViewModel() {
    sealed class SocketEvent {
        data class ChatMessageEvent(val data: ChatMessage) : SocketEvent()
        data class AnnouncementEvent(val data: Announcement) : SocketEvent()
        data class GameStateEvent(val data: GameState) : SocketEvent()
        data class DrawDataEvent(val data: DrawData) : SocketEvent()
        data class NewWordsEvent(val data: NewWords) : SocketEvent()
        data class ChosenWordEvent(val data: ChosenWord) : SocketEvent()
        data class GameErrorEvent(val data: GameError) : SocketEvent()
        data class RoundDrawInfoEvent(val data: RoundDrawInfo) : SocketEvent()
        object UndoEvent : SocketEvent()

    }


    private val _selectedColorButtonId = MutableStateFlow(R.id.rbBlack)
    val selectedColorButtonId: StateFlow<Int>
        get() = _selectedColorButtonId


    private val _connectionProgressBar = MutableStateFlow(true)
    val connectionProgressBar: MutableStateFlow<Boolean>
        get() = _connectionProgressBar

    private val _chooseWordOverlayVisible = MutableStateFlow(false)
    val chooseWordOverlayVisible: MutableStateFlow<Boolean>
        get() = _chooseWordOverlayVisible


    private val connectionEventChannel = Channel<WebSocket.Event>()
    val connectionEvent = connectionEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    private val socketEventChannel = Channel<SocketEvent>()
    val socketEvent = socketEventChannel.receiveAsFlow().flowOn(dispatchers.io)


    fun checkRadioButton(id: Int) {
        _selectedColorButtonId.value = id
    }

    fun setChooseWordOverlayVisibility(visible: Boolean) {
        _chooseWordOverlayVisible.value = visible
    }

    fun connectionProgressBarVisibility(visible: Boolean) {
        _connectionProgressBar.value = visible

    }

    fun observeEvent() {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.observeEvents().collect { event ->
                connectionEventChannel.send(event)

            }
        }
    }

    fun observeBaseModel() {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.observeBaseModels().collect { data ->
                when (data) {
                    is DrawData -> socketEventChannel.send(SocketEvent.DrawDataEvent(data))
                    is DrawAction -> {
                        when (data.action) {
                            ACTION_UNDO -> socketEventChannel.send(SocketEvent.UndoEvent)
                        }
                    }
                    is Ping -> sendBaseModel(Ping())

                    is GameError -> socketEventChannel.send(SocketEvent.GameErrorEvent(data))
                }


            }
        }
    }

    fun sendBaseModel(data: BaseModel) {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.sendBaseModel(data)
        }
    }

    init {
        observeBaseModel()
        observeEvent()

    }
}