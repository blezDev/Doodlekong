package com.blez.doodlekong.data.remote.ws

import com.blez.doodlekong.utils.Constants.TYPE_NEW_WORDS


data class NewWords(
    val newWords : List<String>
): BaseModel(TYPE_NEW_WORDS)
