package com.blez.doodlekong.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.blez.doodlekong.R
import com.blez.doodlekong.data.remote.ws.PlayerData
import com.blez.doodlekong.data.remote.ws.Room
import com.blez.doodlekong.databinding.ItemPlayerBinding
import com.blez.doodlekong.databinding.ItemRoomBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlayerAdapter @Inject constructor() : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {
    private lateinit var binding: ItemPlayerBinding

    class PlayerViewHolder( binding: ItemPlayerBinding) : RecyclerView.ViewHolder(binding.root)

    suspend fun updataDataset(newDataset : List<PlayerData>) = with(Dispatchers.Default)
    {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback(){
            override fun getOldListSize(): Int {
              return players.size
            }

            override fun getNewListSize(): Int {
               return newDataset.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return players[oldItemPosition] == newDataset[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
               return  players[oldItemPosition] == newDataset[newItemPosition]
            }

        })
        withContext(Dispatchers.Main)
        {
            players = newDataset
            diff.dispatchUpdatesTo(this@PlayerAdapter)
        }
    }

    var players = listOf<PlayerData>()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_player,
            parent,
            false
        )
        return PlayerViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return players.size
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        var player = players[position]
        binding.apply {
            val playerRankText = "${player.rank}. "
            tvRank.text = playerRankText
            tvScore.text = player.score.toString()
            tvUsername.text = player.username
            ivPencil.isVisible = player.isDrawing
        }

    }


}