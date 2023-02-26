package com.blez.doodlekong.ui.drawing

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blez.doodlekong.R
import com.blez.doodlekong.adapters.ChatMessageAdapter
import com.blez.doodlekong.data.remote.ws.*
import com.blez.doodlekong.databinding.ActivityDrawingBinding
import com.blez.doodlekong.utils.Constants
import com.blez.doodlekong.utils.Constants.DEFAULT_PAINT_THICKNESS
import com.blez.doodlekong.utils.hideKeyboard
import com.google.android.material.snackbar.Snackbar
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawingBinding
    private val drawingViewModel: DrawingViewModel by viewModels()
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView
    private val args : DrawingActivityArgs by  navArgs()
    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private var updateChatJob : Job?= null

    @Inject
    lateinit var clientId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_drawing)
        subscribeToUiStateUpdates()
        listenToConnectionEvents()
        listenToSocketEvents()
        setupRecyclerView()

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

    }

    private fun subscribeToUiStateUpdates() {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}