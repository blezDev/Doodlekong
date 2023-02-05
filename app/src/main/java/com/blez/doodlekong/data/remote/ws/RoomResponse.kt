package com.blez.doodlekong.data.remote.ws

data class RoomResponse(
    val name : String,
    val maxPlayers : Int,
    val playerCount : Int
)
