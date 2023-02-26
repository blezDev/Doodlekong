package com.blez.doodlekong.utils

import kotlinx.coroutines.*

class CoroutineTimer {
    fun timeAndEmit(
        duration : Long,
        coroutineScope: CoroutineScope,
        emissionfrequency : Long = 100L,
        dispatcher : CoroutineDispatcher = Dispatchers.Main,
        onEmit : (Long) -> Unit

    ) : Job{
        return coroutineScope.launch(dispatcher) {
            var time = duration
            while (time>= 0){
                onEmit(time)
                time -= emissionfrequency
                delay(emissionfrequency)
            }
        }

        
    }
}