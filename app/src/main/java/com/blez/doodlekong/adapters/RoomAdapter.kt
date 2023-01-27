package com.blez.doodlekong.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.blez.doodlekong.R
import com.blez.doodlekong.data.remote.ws.Room
import com.blez.doodlekong.databinding.ItemRoomBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomAdapter @Inject constructor() : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {
    private lateinit var binding: ItemRoomBinding

    class RoomViewHolder(binding: ItemRoomBinding) : RecyclerView.ViewHolder(binding.root)

    suspend fun updataDataset(newDataset : List<Room>) = with(Dispatchers.Default)
    {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback(){
            override fun getOldListSize(): Int {
              return rooms.size
            }

            override fun getNewListSize(): Int {
               return newDataset.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return rooms[oldItemPosition] == newDataset[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
               return  rooms[oldItemPosition] == newDataset[newItemPosition]
            }

        })
        withContext(Dispatchers.Main)
        {
            rooms = newDataset
            diff.dispatchUpdatesTo(this@RoomAdapter)
        }
    }

    var rooms = listOf<Room>()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_room,
            parent,
            false
        )
        return RoomViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        var room = rooms[position]
        binding.apply {
            tvRoomName.text = room.name
            val playerCountText = "${room.players}/ ${room.maxPlayer}"
            tvRoomPersonCount.text = playerCountText
            root.setOnClickListener {
                onRoomClickListener?.let { click ->
                    click(room)
                }
            }

        }
    }

    private var onRoomClickListener: ((Room) -> Unit)? = null

    fun setOnRoomClickListener(listener: ((Room) -> Unit)) {
        onRoomClickListener = listener
    }
}