package com.blez.doodlekong.data.remote.ws

import com.blez.doodlekong.utils.Constants.TYPE_PHASE_CHANGE


data class PhaseChange(
    var phase : Room.Phase?,
    var time : Long,
    val drawingPlayer : String?=null
): BaseModel(TYPE_PHASE_CHANGE)
