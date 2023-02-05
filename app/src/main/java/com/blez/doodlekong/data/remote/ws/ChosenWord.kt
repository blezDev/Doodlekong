package com.blez.doodlekong.data.remote.ws

import com.blez.doodlekong.utils.Constants.TYPE_CHOSEN_WORD


data class ChosenWord(
    val chosenWord: String,
    val roomName : String
): BaseModel(TYPE_CHOSEN_WORD)
