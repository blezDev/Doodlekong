package com.blez.doodlekong.data.remote.ws

import com.blez.doodlekong.utils.Constants.TYPE_JOIN_ROOM_HANDSHAKE


data class JoinRoomHandshake(
    val username : String,
    val roomName : String,
    val clientId : String
): BaseModel(TYPE_JOIN_ROOM_HANDSHAKE)
