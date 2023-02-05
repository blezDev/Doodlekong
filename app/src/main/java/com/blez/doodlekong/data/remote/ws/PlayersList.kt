package com.blez.doodlekong.data.remote.ws

import com.blez.doodlekong.utils.Constants.TYPE_PLAYERS_LIST


data class PlayersList(
    val players : List<PlayerData>
): BaseModel(TYPE_PLAYERS_LIST)
