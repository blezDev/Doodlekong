package com.blez.doodlekong.ui.drawing

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.blez.doodlekong.R
import com.blez.doodlekong.databinding.ActivityDrawingBinding
import com.blez.doodlekong.utils.Constants.DEFAULT_PAINT_THICKNESS
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawingBinding
    private val drawingViewModel : DrawingViewModel by viewModels()
    private lateinit var toggle : ActionBarDrawerToggle
private lateinit var rvPlayers : RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_drawing)
        subscribeToUiStateUpdates()

            toggle = ActionBarDrawerToggle(this,binding.drawerLayout,R.string.open,R.string.close)
        toggle.syncState()

        val header = layoutInflater.inflate(R.layout.nav_drawer_header,binding.navView)
        rvPlayers = header.findViewById(R.id.rvPlayers)
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        binding.ibPlayers.setOnClickListener {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.drawerLayout.addDrawerListener(object: DrawerLayout.DrawerListener{
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) {
               binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            override fun onDrawerStateChanged(newState: Int) = Unit

        })

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}