package com.blez.doodlekong.ui.drawing

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.blez.doodlekong.R
import com.blez.doodlekong.databinding.ActivityDrawingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawingBinding
    private val drawingViewModel : DrawingViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_drawing)
        subscribeToUiStateUpdates()


        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
        drawingViewModel.checkRadioButton(checkedId)
        }

    }

    private fun subscribeToUiStateUpdates()
    {
        lifecycleScope.launchWhenStarted {
            drawingViewModel.selectedColorButtonId.collect{id->
                binding.colorGroup.check(id)
            }
        }
    }
}