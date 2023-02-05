package com.blez.doodlekong.data.remote.ws

import com.blez.doodlekong.utils.Constants.TYPE_GAME_STATE


data class GameState(
    val drawingPlayer : String,
    val word : String
): BaseModel(TYPE_GAME_STATE)
