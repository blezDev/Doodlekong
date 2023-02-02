package com.blez.doodlekong.ui.drawing

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.blez.doodlekong.R
import com.blez.doodlekong.databinding.ActivityDrawingBinding
import com.blez.doodlekong.utils.Constants.DEFAULT_PAINT_THICKNESS
import dagger.hilt.android.AndroidEntryPoint

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
                when(id){
                    R.id.rbBlack->selectColor(Color.BLACK)
                    R.id.rbRed->selectColor(Color.RED)
                    R.id.rbGreen->selectColor(Color.GREEN)
                    R.id.rbBlue->selectColor(Color.BLUE)
                    R.id.rbOrange->selectColor(getColor(R.color.orange))
                    R.id.rbYellow->selectColor(Color.YELLOW)
                    R.id.rbEraser->{
                        selectColor(Color.WHITE)
                        binding.drawingView.setThickness(40f)
                    }
                }
            }
        }
    }

    private fun selectColor(color: Int)
    {
        binding.drawingView.setColor(color)
        binding.drawingView.setThickness(DEFAULT_PAINT_THICKNESS)
    }
}