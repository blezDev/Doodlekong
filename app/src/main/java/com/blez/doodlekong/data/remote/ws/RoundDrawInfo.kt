package com.blez.doodlekong.data.remote.ws

import com.blez.doodlekong.utils.Constants.TYPE_CURRENT_ROUND_DRAW_INFO


data class RoundDrawInfo(
    val data : List<String>
): BaseModel(TYPE_CURRENT_ROUND_DRAW_INFO)
