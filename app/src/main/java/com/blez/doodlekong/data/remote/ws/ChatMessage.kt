package com.blez.doodlekong.data.remote.ws

import com.blez.doodlekong.utils.Constants.TYPE_CHAT_MESSAGE


data class ChatMessage(
    val from : String,
    val roomName : String,
    val message : String,
    val timeStamp : Long
) : BaseModel(TYPE_CHAT_MESSAGE)
