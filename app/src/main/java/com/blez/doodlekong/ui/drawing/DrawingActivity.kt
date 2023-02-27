package com.blez.doodlekong.ui.drawing

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blez.doodlekong.R
import com.blez.doodlekong.adapters.ChatMessageAdapter
import com.blez.doodlekong.adapters.PlayerAdapter
import com.blez.doodlekong.data.remote.ws.*
import com.blez.doodlekong.databinding.ActivityDrawingBinding
import com.blez.doodlekong.ui.dialogs.LeaveDialog
import com.blez.doodlekong.utils.Constants
import com.blez.doodlekong.utils.Constants.DEFAULT_PAINT_THICKNESS
import com.blez.doodlekong.utils.Constants.MAX_WORD_VOICE_GUESS_AMOUNT
import com.blez.doodlekong.utils.Constants.REQUEST_CODE_RECORD_AUDIO
import com.blez.doodlekong.utils.hideKeyboard
import com.google.android.material.snackbar.Snackbar
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity(), LifecycleObserver , EasyPermissions.PermissionCallbacks, RecognitionListener {
    private lateinit var binding: ActivityDrawingBinding
    private val drawingViewModel: DrawingViewModel by viewModels()
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView
    private val args : DrawingActivityArgs by  navArgs()
    private lateinit var chatMessageAdapter: ChatMessageAdapter
    @Inject
     lateinit var playerAdapter: PlayerAdapter
    private var updateChatJob : Job?= null
    private var updatePlayerJob : Job?= null

    private lateinit var speechRecognizer : SpeechRecognizer
    private lateinit var speechIntent : Intent



    @Inject
    lateinit var clientId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_drawing)
        subscribeToUiStateUpdates()
        listenToConnectionEvents()
        listenToSocketEvents()
        setupRecyclerView()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.open, R.string.close)
        toggle.syncState()

        binding.drawingView.roomName  = args.roomName
        chatMessageAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        val header = layoutInflater.inflate(R.layout.nav_drawer_header, binding.navView)
        rvPlayers = header.findViewById(R.id.rvPlayers)
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        binding.ibPlayers.setOnClickListener {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            override fun onDrawerStateChanged(newState: Int) = Unit

        })

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent= Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.US)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,MAX_WORD_VOICE_GUESS_AMOUNT)
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)){
            speechRecognizer.setRecognitionListener(this)
        }

       rvPlayers.apply {
           adapter = playerAdapter
           layoutManager = LinearLayoutManager(this@DrawingActivity)
       }
        binding.ibUndo.setOnClickListener {
            if (binding.drawingView.isUserDrawing){
                binding.drawingView.undo()
                drawingViewModel.sendBaseModel(DrawAction(DrawAction.ACTION_UNDO))
            }
        }

        binding.ibClearText.setOnClickListener {
            binding.etMessage.text?.clear()
        }
        binding.ibSend.setOnClickListener {
            drawingViewModel.sendChatMessage(
                ChatMessage(args.username,args.roomName,binding.etMessage.text.toString(),System.currentTimeMillis())
            )
            binding.etMessage.text?.clear()
            hideKeyboard(binding.root)
        }




        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            drawingViewModel.checkRadioButton(checkedId)
        }
        binding.drawingView.setOnDrawListener {
            if (binding.drawingView.isUserDrawing){
                drawingViewModel.sendBaseModel(data = it)
            }
        }
        binding.drawingView.setOnPathDataChangedListener {
            drawingViewModel.setPathData(it)
        }


        binding.ibMic.setOnClickListener {
            if (drawingViewModel.speechToTextEnabled.value && hasRecordAudioPermission())
            {
                drawingViewModel.startListening()
            }else if (drawingViewModel.speechToTextEnabled.value){
                requestRecordAudioPermission()
            }else{
                drawingViewModel.stopListening()
            }
        }


        var lifecycleEventObserver = LifecycleEventObserver{_, event ->
            when(event){
                Lifecycle.Event.ON_STOP->{
                   onAppInBackground()
                }

                else -> {}
            }

        }
        lifecycleEventObserver










    }




    private fun subscribeToUiStateUpdates() {
        lifecycleScope.launchWhenStarted {
            drawingViewModel.speechToTextEnabled.collect{isEnabled->
                if(isEnabled && !SpeechRecognizer.isRecognitionAvailable(this@DrawingActivity)){
                    Toast.makeText(this@DrawingActivity, R.string.speech_not_available, Toast.LENGTH_LONG).show()
                    binding.ibMic.isEnabled = false
                }else
                    setSpeechRecognitionEnabled(isEnabled)
            }

        }

        lifecycleScope.launchWhenStarted {
            drawingViewModel.pathData.collect{pathData->
           binding.drawingView.setPaths(pathData)
                }
            }


        lifecycleScope.launchWhenStarted {
            drawingViewModel.chat.collect{chat->
            if(chatMessageAdapter.chatObjects.isEmpty()){
                updateChatMessageList(chat)
            }
            }
        }
        lifecycleScope.launchWhenStarted {
            drawingViewModel.newWords.collect{
                val newWords = it.newWords
                if(newWords.isEmpty()){
                    return@collect
                }
                binding.apply {
                    btnFirstWord.text = newWords[0]
                    btnSecondWord.text = newWords[1]
                    btnThirdWord.text = newWords[2]
                    btnFirstWord.setOnClickListener {

                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            drawingViewModel.selectedColorButtonId.collect { id ->
                binding.colorGroup.check(id)
                when (id) {
                    R.id.rbBlack -> selectColor(Color.BLACK)
                    R.id.rbRed -> selectColor(Color.RED)
                    R.id.rbGreen -> selectColor(Color.GREEN)
                    R.id.rbBlue -> selectColor(Color.BLUE)
                    R.id.rbOrange -> selectColor(getColor(R.color.orange))
                    R.id.rbYellow -> selectColor(Color.YELLOW)
                    R.id.rbEraser -> {
                        selectColor(Color.WHITE)
                        binding.drawingView.setThickness(40f)
                    }
                }
            }

        }
    lifecycleScope.launchWhenStarted {
        drawingViewModel.phaseTime.collect{time->
            binding.roundTimerProgressBar.progress = time.toInt()
            binding.tvRemainingTimeChooseWord.text = (time /1000L).toString()


        }
    }


        lifecycleScope.launchWhenStarted {
            drawingViewModel.players.collect{players->
                updatePlayersList(players)


            }
        }


        lifecycleScope.launchWhenStarted {
            drawingViewModel.gameState.collect{gameState->
              binding.apply {
                 tvCurWord.text  = gameState.word
                 val isUserDrawing = gameState.drawingPlayer == args.username
                  setColorGroupVisibility(isUserDrawing)
                  setMessageInputVisibility(!isUserDrawing)
                  drawingView.isUserDrawing = isUserDrawing
                  ibMic.isVisible = !isUserDrawing
                  ibUndo.isEnabled = isUserDrawing
                  drawingView.isEnabled = isUserDrawing
              }


            }
        }


        lifecycleScope.launchWhenStarted {
            drawingViewModel.phase.collect{phase->
               when(phase.phase){
                   Room.Phase.WAITING_FOR_PLAYERS->{
                       binding.tvCurWord.text = getString(R.string.waiting_for_players)
                       drawingViewModel.cancelTimer()
                       drawingViewModel.connectionProgressBarVisibility(false)
                       binding.roundTimerProgressBar.progress = binding.roundTimerProgressBar.max

                   }
                   Room.Phase.WAITING_FOR_START->{
                       binding.roundTimerProgressBar.max = phase.time.toInt()
                       binding.tvCurWord.text = getString(R.string.waiting_for_start)
                   }
                   Room.Phase.NEW_ROUND->{
                       phase.drawingPlayer?.let{
                           binding.tvCurWord.text = getString(R.string.player_is_drawing,it)
                       }
                       binding.apply {
                           drawingView.isEnabled = false
                           drawingView.setColor(Color.BLACK)
                           drawingView.setThickness(DEFAULT_PAINT_THICKNESS)
                           roundTimerProgressBar.max = phase.time.toInt()
                           val isUserDrawingPlayer = phase.drawingPlayer == args.username
                           binding.chooseWordOverlay.isVisible= isUserDrawingPlayer
                       }

                   }
                   Room.Phase.GAME_RUNNING->{
                       binding.chooseWordOverlay.isVisible = false
                       binding.roundTimerProgressBar.max = phase.time.toInt()

                   }
                   Room.Phase.SHOW_WORD->{
                       binding.apply {
                           if (drawingView.isDrawing){
                               drawingView.finishOffDrawing()
                           }
                           drawingView.isEnabled = false
                           drawingView.setColor(Color.BLACK)
                           drawingView.setThickness(DEFAULT_PAINT_THICKNESS)
                           roundTimerProgressBar.max = phase.time.toInt()
                       }
                   }

                   else -> Unit

               }


            }
        }

        lifecycleScope.launchWhenStarted {
            drawingViewModel.connectionProgressBar.collect{isVisible->
                binding.connectionProgressBar.isVisible = isVisible
            }
        }
        lifecycleScope.launchWhenStarted {
            drawingViewModel.chooseWordOverlayVisible.collect{isVisible->
                binding.chooseWordOverlay.isVisible = isVisible
            }
        }

    }

    private fun setColorGroupVisibility(isVisible : Boolean){
        binding.colorGroup.isVisible = isVisible
        binding.ibUndo.isVisible = isVisible
    }

    private fun setMessageInputVisibility(isVisible: Boolean){
        binding.apply {
            tilMessage.isVisible = isVisible
            ibSend.isVisible = isVisible
            ibClearText.isVisible = isVisible
        }
    }



    private fun listenToSocketEvents() = lifecycleScope.launchWhenStarted {
        drawingViewModel.socketEvent.collect{event->
            when(event){
                is DrawingViewModel.SocketEvent.DrawDataEvent->{
                    val drawData = event.data
                    if(!binding.drawingView.isUserDrawing){
                        when(drawData.motionEvent){
                            MotionEvent.ACTION_DOWN -> binding.drawingView.startedTouchExternally(drawData)
                            MotionEvent.ACTION_MOVE->binding.drawingView.movedTouchExternally(drawData)
                            MotionEvent.ACTION_UP->binding.drawingView.releasedTouchExternally(drawData)

                        }
                    }
                }

                is DrawingViewModel.SocketEvent.GameStateEvent->{
                    binding.drawingView.clear()


                }

                is DrawingViewModel.SocketEvent.RoundDrawInfoEvent->{
                    binding.drawingView.update(event.data)
                }


                is DrawingViewModel.SocketEvent.ChatMessageEvent->{
                    addChatObjectToRecyclerView(event.data)
                }
                is DrawingViewModel.SocketEvent.AnnouncementEvent->{
                    addChatObjectToRecyclerView(event.data)
                }
                is DrawingViewModel.SocketEvent.ChosenWordEvent->{
                    binding.tvCurWord.text = event.data.chosenWord
                    binding.ibUndo.isEnabled = false
                }

                is DrawingViewModel.SocketEvent.GameErrorEvent->{
                    when(event.data.errorType){
                        GameError.ERROR_ROOM_NOT_FOUND-> finish()
                    }
                }
                is DrawingViewModel.SocketEvent.UndoEvent->{
                    binding.drawingView.undo()
                }
                else->Unit
            }

        }
    }


    private fun hasRecordAudioPermission() = EasyPermissions.hasPermissions(
        this,
       Manifest.permission.RECORD_AUDIO
    )
    private fun requestRecordAudioPermission(){
        EasyPermissions.requestPermissions(
            this@DrawingActivity,
            getString(R.string.rationale_record_audio),
            REQUEST_CODE_RECORD_AUDIO,
            Manifest.permission.RECORD_AUDIO
        )
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
      if (requestCode == REQUEST_CODE_RECORD_AUDIO){
          Toast.makeText(this,R.string.speech_to_text_info,Toast.LENGTH_LONG).show()
      }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun setSpeechRecognitionEnabled(isEnabled : Boolean){
        if (isEnabled){
            binding.ibMic.setImageResource(R.drawable.ic_mic)
            speechRecognizer.startListening(speechIntent)
        }else
        {
            binding.ibMic.setImageResource(R.drawable.ic_mic_off)
            binding.etMessage.hint = " "
            speechRecognizer.stopListening()
        }
    }


    override fun onReadyForSpeech(params: Bundle?) {
      binding.etMessage.text?.clear()
        binding.etMessage.hint = getString(R.string.listening)
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float)  = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
       drawingViewModel.stopListening()
    }

    override fun onError(error: Int)  = Unit

    override fun onResults(results: Bundle?) {
      val strings = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val guessedWords = strings?.joinToString(" ")
        guessedWords?.let {
            binding.etMessage.setText(guessedWords)
        }
        speechRecognizer.stopListening()
        drawingViewModel.stopListening()
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun selectColor(color: Int) {
        binding.drawingView.setColor(color)
        binding.drawingView.setThickness(DEFAULT_PAINT_THICKNESS)
    }
    private fun listenToConnectionEvents() = lifecycleScope.launchWhenStarted {
        drawingViewModel.connectionEvent.collect{event->
            when(event){
                is WebSocket.Event.OnConnectionOpened<*>->{
                    drawingViewModel.sendBaseModel(JoinRoomHandshake(
                        args.username,args.roomName,clientId
                    ))
                    drawingViewModel.connectionProgressBarVisibility(false)
                }
                is WebSocket.Event.OnConnectionFailed->{
                    drawingViewModel.connectionProgressBarVisibility(false)
                    Snackbar.make(binding.root,R.string.error_connection_failed,Snackbar.LENGTH_LONG).show()
                    event.throwable.printStackTrace()
                }
                is WebSocket.Event.OnConnectionClosed->{
                    drawingViewModel.connectionProgressBarVisibility(false)
                }
              else->Unit
            }

        }
    }

    private fun setupRecyclerView() = binding.rvChat.apply {
        chatMessageAdapter = ChatMessageAdapter(args.username)
        adapter = chatMessageAdapter
        layoutManager = LinearLayoutManager(this@DrawingActivity)
    }

    private fun updateChatMessageList(chat : List<BaseModel>){
      updateChatJob?.cancel()
      updateChatJob = lifecycleScope.launch {
          chatMessageAdapter.updataDataset(chat)
      }
    }
    private suspend fun addChatObjectToRecyclerView(chatObject : BaseModel){
      val canScrollDown = binding.rvChat.canScrollVertically(1)
        updateChatMessageList(chatMessageAdapter.chatObjects + chatObject)
        updateChatJob?.join()
        if (!canScrollDown){
            binding.rvChat.scrollToPosition(chatMessageAdapter.chatObjects.size -1)
        }

    }

    override fun onPause() {
        super.onPause()
        binding.rvChat.layoutManager?.onSaveInstanceState()
    }

    private fun updatePlayersList(players : List<PlayerData>){
        updatePlayerJob?.cancel()
        updatePlayerJob = lifecycleScope.launch {
            playerAdapter.updataDataset(players)
        }
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }



private fun onAppInBackground(){
    drawingViewModel.disconnect()
}


    override fun onBackPressed() {
        LeaveDialog().apply {
            setPositiveClickListener {
             drawingViewModel.disconnect()
                finish()
            }

        }.show(supportFragmentManager,null)

    }



}