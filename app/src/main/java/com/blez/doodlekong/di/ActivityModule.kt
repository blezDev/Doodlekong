package com.blez.doodlekong.di

import android.app.Application
import android.content.Context
import com.blez.doodlekong.data.remote.api.SetupApi
import com.blez.doodlekong.data.remote.ws.CustomGsonMessageAdapter
import com.blez.doodlekong.data.remote.ws.DrawingApi
import com.blez.doodlekong.data.remote.ws.FlowStreamAdapter
import com.blez.doodlekong.ui.drawing.repository.DefaultSetupRepositoryImpl
import com.blez.doodlekong.ui.drawing.repository.SetupRepository
import com.blez.doodlekong.utils.Constants
import com.google.gson.Gson
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(ActivityRetainedComponent::class)
object ActivityModule {
    @ActivityRetainedScoped
    @Provides
    fun providesDrawingApi(app : Application, okHttpClient: OkHttpClient, gson: Gson) : DrawingApi {
        return Scarlet.Builder()
            .backoffStrategy(LinearBackoffStrategy(Constants.RECONNECT_INTERVAL))
            .lifecycle(AndroidLifecycle.ofApplicationForeground(app))
            .webSocketFactory(okHttpClient.newWebSocketFactory(if (Constants.USE_LOCALHOST) Constants.WS_BASE_URL_LOCAL_HOST else Constants.WS_BASE_URL))
            .addStreamAdapterFactory(FlowStreamAdapter.Factory)
            .addMessageAdapterFactory(CustomGsonMessageAdapter.Factory(gson))
            .build()
            .create(DrawingApi::class.java)
    }




    @ActivityRetainedScoped
    @Provides
    fun providesSetupApi(okHttpClient: OkHttpClient) : SetupApi {
        return Retrofit.Builder()
            .baseUrl(if (Constants.USE_LOCALHOST) Constants.HTTP_BASE_URL_LOCAL_HOST else Constants.HTTP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SetupApi::class.java)
    }

    @ActivityRetainedScoped
    @Provides
    fun providesSetupRepository(setupApi: SetupApi,@ApplicationContext context: Context) : SetupRepository = DefaultSetupRepositoryImpl(setupApi, context)


}