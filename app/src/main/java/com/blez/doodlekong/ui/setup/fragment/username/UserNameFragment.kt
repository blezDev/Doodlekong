package com.blez.doodlekong.ui.setup.fragment.username

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.blez.doodlekong.R
import com.blez.doodlekong.databinding.FragmentUserNameBinding
import com.blez.doodlekong.ui.setup.UserViewModel
import com.blez.doodlekong.utils.Constants
import com.blez.doodlekong.utils.navigateSafely
import com.blez.doodlekong.utils.snakeBar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class UserNameFragment : Fragment() {
    private lateinit var binding: FragmentUserNameBinding
    private val userNameViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_name, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToEvents()
        binding.btnNext.setOnClickListener {
            userNameViewModel.validateUsernameAndNavigateToSelectRoom(
                binding.etUsername.text.toString()
            )

        }

    }

    private fun listenToEvents() {
        lifecycleScope.launchWhenCreated {
            userNameViewModel.setupEvent.collect { event ->
                when (event) {
                    is UserViewModel.SetupEvent.NavigateToSelectRoomEvent -> {
                        findNavController().navigateSafely(R.id.action_userNameFragment_to_selectRoomFragment,
                        args = Bundle().apply
                         { putString("username",event.username) })

                    }
                    is UserViewModel.SetupEvent.InputEmpty ->{
                        snakeBar(getString(R.string.error_field_empty))

                    }
                    is UserViewModel.SetupEvent.InputTooShortError->{
                        snakeBar(getString(R.string.error_room_name_too_short,Constants.MIN_ROOM_NAME_LENGTH))
                    }
                    is UserViewModel.SetupEvent.InputTooLongError->{
                        snakeBar(getString(R.string.error_room_name_too_long,Constants.MAX_USERNAME_LENGTH))
                    }
                    else -> Unit

                }
            }
        }
    }


}