package com.blez.doodlekong.ui.drawing.repository

import com.blez.doodlekong.data.remote.responses.BasicApiResponse
import com.blez.doodlekong.data.remote.ws.Room
import com.blez.doodlekong.utils.Resource

interface SetupRepository {
    suspend fun createRoom(room: Room) : Resource<Unit>
    suspend fun getRoom(searchQuery : String) : Resource<List<Room>>
    suspend fun joinRoom(username : String,roomName : String): Resource<Unit>
}