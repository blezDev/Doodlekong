package com.blez.doodlekong.adapters


import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.blez.doodlekong.R
import com.blez.doodlekong.data.remote.ws.Announcement
import com.blez.doodlekong.data.remote.ws.BaseModel
import com.blez.doodlekong.data.remote.ws.ChatMessage
import com.blez.doodlekong.databinding.ItemAnnouncementBinding
import com.blez.doodlekong.databinding.ItemChatMessageIncomingBinding
import com.blez.doodlekong.databinding.ItemChatMessageOutgoingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

private const val VIEW_TYPE_INCOMING_MESSAGE = 0
private const val VIEW_TYPE_OUTGOING_MESSAGE = 1
private const val VIEW_TYPE_ANNOUNCEMENT_INCOMING_MESSAGE = 2

class ChatMessageAdapter(private val username : String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var incomingBinding : ItemChatMessageIncomingBinding
    private lateinit var outgoingBinding: ItemChatMessageOutgoingBinding
    private lateinit var announcementBinding : ItemAnnouncementBinding
    class IncomingChatMessageViewHolder(incomingBinding : ItemChatMessageIncomingBinding) : RecyclerView.ViewHolder(incomingBinding.root)
    class OutGoingChatMessageViewHolder(outgoingBinding: ItemChatMessageOutgoingBinding) : RecyclerView.ViewHolder(outgoingBinding.root)
    class AnnouncementChatMessageViewHolder(announcementBinding : ItemAnnouncementBinding) : RecyclerView.ViewHolder(announcementBinding.root)



    var chatObjects = listOf<BaseModel>()

    suspend fun updataDataset(newDataset : List<BaseModel>) = with(Dispatchers.Default)
    {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback(){
            override fun getOldListSize(): Int {
                return chatObjects.size
            }

            override fun getNewListSize(): Int {
                return newDataset.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return chatObjects[oldItemPosition] == newDataset[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return  chatObjects[oldItemPosition] == newDataset[newItemPosition]
            }

        })
        withContext(Dispatchers.Main)
        {
            chatObjects = newDataset
            diff.dispatchUpdatesTo(this@ChatMessageAdapter)
        }

    }

    override fun getItemViewType(position: Int): Int {

        return when(val obj = chatObjects[position]){
            is Announcement -> VIEW_TYPE_ANNOUNCEMENT_INCOMING_MESSAGE
            is ChatMessage->{
                if(username == obj.from){
                    VIEW_TYPE_OUTGOING_MESSAGE
                }
                else
                    VIEW_TYPE_INCOMING_MESSAGE
            }
            else -> throw IllegalStateException("Unknown ViewType")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
     return when(viewType){
         VIEW_TYPE_INCOMING_MESSAGE->{
         incomingBinding =DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_chat_message_incoming,parent,false)
             IncomingChatMessageViewHolder(incomingBinding)
         }
         VIEW_TYPE_OUTGOING_MESSAGE->{
            outgoingBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_chat_message_outgoing,parent,false)
             OutGoingChatMessageViewHolder(outgoingBinding)
         }
         VIEW_TYPE_ANNOUNCEMENT_INCOMING_MESSAGE->{
           announcementBinding =  DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_announcement,parent,false)
             AnnouncementChatMessageViewHolder(announcementBinding)
         }
         else -> throw IllegalStateException("Unknow ViewType")
     }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is AnnouncementChatMessageViewHolder->{
                val announcement = chatObjects[position] as Announcement
                announcementBinding.apply {
                    tvAnnouncement.text = announcement.message
                    val dataFormat = SimpleDateFormat("kk:mm:ss", Locale.getDefault())
                    val date = dataFormat.format(announcement.timestamp)
                    tvTime.text = date
                    when(announcement.announcementType){
                        Announcement.TYPE_EVERYBODY_GUSSED_IT->{
                            root.setBackgroundColor(Color.LTGRAY)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)

                        }
                        Announcement.TYPE_PLAYER_JOINED->{
                            root.setBackgroundColor(Color.GREEN)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)

                        }
                        Announcement.TYPE_PLAYER_GUESSED_WORD->{
                            root.setBackgroundColor(Color.YELLOW)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)

                        }
                        Announcement.TYPE_PLAYER_LEFT->{
                            root.setBackgroundColor(Color.RED)
                            tvAnnouncement.setTextColor(Color.WHITE)
                            tvTime.setTextColor(Color.WHITE)

                        }
                    }
                }
            }
            is IncomingChatMessageViewHolder->{
                val message = chatObjects[position] as ChatMessage
                incomingBinding.apply {
                    tvMessage.text = message.message
                    tvUsername.text = message.from
                    val dataFormat = SimpleDateFormat("kk:mm:ss", Locale.getDefault())
                    val date = dataFormat.format(message.timeStamp)
                    tvTime.text = date
                }
            }
            is OutGoingChatMessageViewHolder->{
                val message = chatObjects[position] as ChatMessage
                    outgoingBinding.apply {
                        tvMessage.text = message.message
                        tvUsername.text = message.from
                        val dataFormat = SimpleDateFormat("kk:mm:ss", Locale.getDefault())
                        val date = dataFormat.format(message.timeStamp)
                        tvTime.text = date
                    }
            }
            else -> throw IllegalStateException("Unknown ViewHolder")
        }
    }




    override fun getItemCount(): Int {
        return chatObjects.size
    }


}