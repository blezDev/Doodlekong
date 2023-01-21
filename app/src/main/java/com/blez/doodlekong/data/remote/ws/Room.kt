package com.blez.doodlekong.data.remote.ws

data class Room(
    val name : String,
    val maxPlayer : Int,
    val players : Int = 1
){
    enum class Phase{
        WAITING_FOR_PLAYERS,
        WAITING_FOR_START,
        NEW_ROUND,
        GAME_RUNNING,
        SHOW_WORD

    }
}
