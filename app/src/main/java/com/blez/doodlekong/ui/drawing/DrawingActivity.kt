package com.blez.doodlekong.ui.drawing

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.blez.doodlekong.R
import com.blez.doodlekong.databinding.ActivityDrawingBinding

class DrawingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_drawing)

    }
}