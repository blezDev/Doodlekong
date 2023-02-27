package com.blez.doodlekong.ui.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blez.doodlekong.R
import com.blez.doodlekong.data.remote.ws.*
import com.blez.doodlekong.data.remote.ws.DrawAction.Companion.ACTION_UNDO
import com.blez.doodlekong.ui.views.DrawingView
import com.blez.doodlekong.utils.Constants
import com.blez.doodlekong.utils.CoroutineTimer
import com.blez.doodlekong.utils.DispatcherProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
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
        data class RoundDrawInfoEvent(val data: List<BaseModel>) : SocketEvent()
        object UndoEvent : SocketEvent()

    }
    private val _newWords = MutableStateFlow(NewWords(listOf()))
    val newWords: StateFlow<NewWords>
        get() = _newWords

    private val timer = CoroutineTimer()
    private var timerjob : Job? = null


    private val _phase = MutableStateFlow(PhaseChange(null,0L,null))
    val phase: StateFlow<PhaseChange>
        get() = _phase

    private val _phaseTime = MutableStateFlow(0L)
    val phaseTime  : StateFlow<Long>
        get() = _phaseTime

    private val _pathData = MutableStateFlow(Stack<DrawingView.PathData>())
    val pathData : StateFlow<Stack<DrawingView.PathData>>
    get() = _pathData


    private val _players = MutableStateFlow<List<PlayerData>>(listOf())
    val players : StateFlow<List<PlayerData>>
    get() = _players






    private fun setTimer(duration : Long){
        timerjob?.cancel()
        timerjob = timer.timeAndEmit(duration,viewModelScope){
        _phaseTime.value = it
        }
    }







    private val _chat = MutableStateFlow<List<BaseModel>>(listOf())
    val chat: StateFlow<List<BaseModel>>
    get() = _chat



    private val _selectedColorButtonId = MutableStateFlow(R.id.rbBlack)
    val selectedColorButtonId: StateFlow<Int>
        get() = _selectedColorButtonId


    private val _connectionProgressBar = MutableStateFlow(true)
    val connectionProgressBar: MutableStateFlow<Boolean>
        get() = _connectionProgressBar



    private val _speechToTextEnabled = MutableStateFlow(true)
    val speechToTextEnabled: MutableStateFlow<Boolean>
        get() = _speechToTextEnabled




    private val _chooseWordOverlayVisible = MutableStateFlow(false)
    val chooseWordOverlayVisible: MutableStateFlow<Boolean>
        get() = _chooseWordOverlayVisible



    private val _gameState = MutableStateFlow<GameState>(GameState("",""))
    val gameState: StateFlow<GameState>
        get() = _gameState


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

    fun startListening( ){
        _speechToTextEnabled.value = true

    }
    fun stopListening( ){
        _speechToTextEnabled.value = false

    }

    fun cancelTimer(){
        timerjob?.cancel()
    }
    fun setPathData(stack : Stack<DrawingView.PathData>){
        _pathData.value = stack
    }
    fun observeBaseModel() {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.observeBaseModels().collect { data ->
                when (data) {
                    is DrawData -> socketEventChannel.send(SocketEvent.DrawDataEvent(data))

                    is ChatMessage->{
                       socketEventChannel.send(SocketEvent.ChatMessageEvent(data))
                    }

                    is Announcement->{
                        socketEventChannel.send(SocketEvent.AnnouncementEvent(data))
                    }
                    is PhaseChange->{
                       data.phase?.let {
                           _phase.value = data
                       }
                        _phaseTime.value = data.time
                        if(data.phase != Room.Phase.WAITING_FOR_PLAYERS)
                        {
                            setTimer(data.time)
                        }
                    }

                    is RoundDrawInfo->{
                        val drawActions = mutableListOf<BaseModel>()
                        data.data.forEach {drawAction->
                            val jsonObject = JsonParser.parseString(drawAction).asJsonObject
                         val type =   when(jsonObject.get("type").asString){
                                Constants.TYPE_DRAW_DATA-> DrawData::class.java
                                Constants.TYPE_DRAW_ACTION-> DrawAction::class.java
                                else -> BaseModel::class.java
                            }
                            drawActions.add(gson.fromJson(drawAction,type))
                        }

                        socketEventChannel.send(SocketEvent.RoundDrawInfoEvent(drawActions))
                    }


                    is NewWords->{
                        _newWords.value = data
                        socketEventChannel.send(SocketEvent.NewWordsEvent(data))
                    }

                    is GameState->{
                        _gameState.value = data
                        socketEventChannel.send(SocketEvent.GameStateEvent(data))
                    }
                    is  PlayersList->{
                        _players.value = data.players
                    }

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
    fun chooseWord(word : String, roomName : String){
        val chosenWord = ChosenWord(word, roomName)
        sendBaseModel(chosenWord)
    }

    fun sendBaseModel(data: BaseModel) {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.sendBaseModel(data)
        }
    }

    fun sendChatMessage(message : ChatMessage){
        if(message.message.trim().isEmpty()){
            return
        }
       viewModelScope.launch(dispatchers.io) {
           drawingApi.sendBaseModel(message)
       }
    }

    fun disconnect(){
        sendBaseModel(DisconnectRequest())
    }

    init {
        observeBaseModel()
        observeEvent()

    }
}