package com.blez.doodlekong.ui.setup.fragment.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blez.doodlekong.R
import com.blez.doodlekong.adapters.RoomAdapter
import com.blez.doodlekong.databinding.FragmentSelectRoomBinding
import com.blez.doodlekong.ui.setup.SelectRoomViewModel
import com.blez.doodlekong.utils.Constants.SEARCH_DELAY
import com.blez.doodlekong.utils.navigateSafely
import com.blez.doodlekong.utils.snakeBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class SelectRoomFragment : Fragment() {
    private lateinit var binding : FragmentSelectRoomBinding
    private val selectRoomViewModel : SelectRoomViewModel by viewModels()
    private val args : SelectRoomFragmentArgs by navArgs()

    @Inject
    lateinit var roomAdapter: RoomAdapter

    var updateRoomJob : Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_select_room, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerview()
        subscribeToObservers()
        listenToEvents()
        selectRoomViewModel.getRooms("")

        var searchJob : Job? = null
        binding.etRoomName.addTextChangedListener{
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(SEARCH_DELAY)
                selectRoomViewModel.getRooms(it.toString())
            }
        }
        binding.ibReload.setOnClickListener {
            binding.roomsProgressBar.isVisible = true
            binding.tvNoRoomsFound.isVisible = false
            binding.ivNoRoomsFound.isVisible = false
            selectRoomViewModel.getRooms(binding.etRoomName.text.toString())
        }
        binding.btnCreateRoom.setOnClickListener {
            findNavController().navigateSafely(R.id.action_selectRoomFragment_to_createRoomFragment,Bundle().apply {
                putString("username",args.username)
            })
        }

        roomAdapter.setOnRoomClickListener {
            selectRoomViewModel.joinRoom(args.username,it.name)
        }
    }

    private fun subscribeToObservers() = lifecycleScope.launchWhenStarted {
        selectRoomViewModel.rooms.collect{events ->
            when(events)
            {
                is SelectRoomViewModel.SetupEvent.GetRoomLoadingEvent->{
                    binding.roomsProgressBar.isVisible = true
                }
                is SelectRoomViewModel.SetupEvent.GetRoomEvent->{
                    binding.roomsProgressBar.isVisible = false
                    val isRoomEmpty  = events.rooms.isEmpty()
                    binding.tvNoRoomsFound.isVisible = isRoomEmpty
                    binding.ivNoRoomsFound.isVisible = isRoomEmpty
                    updateRoomJob?.cancel()
                    updateRoomJob = lifecycleScope.launch {
                        roomAdapter.updataDataset(events.rooms)
                    }
                }
                else-> Unit
            }

        }
    }

    private fun listenToEvents() = lifecycleScope.launchWhenStarted {
        selectRoomViewModel.setupEvent.collect{event->
            when(event)
            {
               is SelectRoomViewModel.SetupEvent.JoinRoomEvent->{
                findNavController().navigateSafely(R.id.action_selectRoomFragment_to_drawingActivity,
                args = Bundle().apply
                 { putString("username",args.username)
                 putString("roomName",event.roomName)})
               }
                is SelectRoomViewModel.SetupEvent.JoinRoomErrorEvent->{
                    snakeBar(event.error)
                }
                is SelectRoomViewModel.SetupEvent.GetRoomErrorEvent->{
                   binding.apply {
                       roomsProgressBar.isVisible = false
                       tvNoRoomsFound.isVisible = false
                       ivNoRoomsFound.isVisible = false
                   }
                    snakeBar(event.error)
                }
                else -> Unit
            }

        }
    }

    private fun setupRecyclerview()
    {
        binding.rvRooms.apply {
            adapter = roomAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }


}