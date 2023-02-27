package com.blez.doodlekong.ui.setup.fragment.createRoom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blez.doodlekong.R
import com.blez.doodlekong.data.remote.ws.Room
import com.blez.doodlekong.databinding.FragmentCreateRoomBinding
import com.blez.doodlekong.ui.setup.CreateRoomViewModel
import com.blez.doodlekong.utils.Constants
import com.blez.doodlekong.utils.hideKeyboard
import com.blez.doodlekong.utils.navigateSafely
import com.blez.doodlekong.utils.snakeBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateRoomFragment : Fragment() {
    private lateinit var binding : FragmentCreateRoomBinding
    private val createRoomViewModel : CreateRoomViewModel by viewModels()
    private val args : CreateRoomFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_create_room, container, false)
        return binding.root
    }

    private fun setUpRoomSizeSpinner(){
        val roomSizes = resources.getStringArray(R.array.room_size_array)
        val adapter = ArrayAdapter(requireContext(),R.layout.textview_room_size,roomSizes)
        binding.tvMaxPersons.setAdapter(adapter)
    }
    private fun listenToEvents()
    {
        lifecycleScope.launch{
            createRoomViewModel.setupEvent.collect{
                when(it)
                {

                  is  CreateRoomViewModel.SetupEvent.CreateRoomEvent->{
                        createRoomViewModel.createRoom(it.room)
                      createRoomViewModel.joinRoom(args.username,it.room.name)
                    }
                    is CreateRoomViewModel.SetupEvent.InputEmptyError->{
                    binding.createRoomProgressBar.isVisible = false
                        snakeBar(R.string.error_field_empty)
                    }
                    is CreateRoomViewModel.SetupEvent.InputTooShortError->{
                        binding.createRoomProgressBar.isVisible = false

                        snakeBar(getString(R.string.error_room_name_too_short, Constants.MIN_ROOM_NAME_LENGTH))
                    }
                    is CreateRoomViewModel.SetupEvent.InputTooLongError->{
                        binding.createRoomProgressBar.isVisible = false
                        snakeBar(getString(R.string.error_room_name_too_long, Constants.MIN_ROOM_NAME_LENGTH))
                    }
                    is CreateRoomViewModel.SetupEvent.CreateRoomErrorEvent->{
                        binding.createRoomProgressBar.isVisible = false
                        snakeBar(it.error)
                    }



                    is CreateRoomViewModel.SetupEvent.JoinRoomEvent->{
                        binding.createRoomProgressBar.isVisible = false
                       findNavController().navigateSafely(R.id.action_createRoomFragment_to_drawingActivity, args = Bundle().apply
                        { putString("username",args.username)
                        putString("roomName",it.roomName)})
                    }

                    is CreateRoomViewModel.SetupEvent.JoinRoomErrorEvent->{
                        binding.createRoomProgressBar.isVisible = false
                        snakeBar(it.error)
                    }
                    else->{
                       Unit
                    }

                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRoomSizeSpinner()
        listenToEvents()
        binding.btnCreateRoom.setOnClickListener {
            binding.createRoomProgressBar.isVisible = true
            createRoomViewModel.createRoom(room = Room(
                name = binding.etRoomName.text.toString(), maxPlayer =
                binding.tvMaxPersons.text.toString().toInt()
            ))
            requireActivity().hideKeyboard(binding.root)

        }

    }
}