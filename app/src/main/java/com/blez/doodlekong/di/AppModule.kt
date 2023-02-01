package com.blez.doodlekong.di

import android.content.Context
import com.blez.doodlekong.data.remote.api.SetupApi
import com.blez.doodlekong.repository.DefaultSetupRepositoryImpl
import com.blez.doodlekong.repository.SetupRepository
import com.blez.doodlekong.utils.Constants.HTTP_BASE_URL
import com.blez.doodlekong.utils.Constants.HTTP_BASE_URL_LOCAL_HOST
import com.blez.doodlekong.utils.Constants.USE_LOCALHOST
import com.blez.doodlekong.utils.DispatcherProvider
import com.blez.doodlekong.utils.clientId
import com.blez.doodlekong.utils.dataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
   fun provideOkHttpClient(clientId : String) : OkHttpClient{
       return OkHttpClient.Builder()
           .addInterceptor{chain ->
               val url = chain.request().url.newBuilder()
                   .addQueryParameter("client_Id",clientId)
                   .build()
               val request = chain.request().newBuilder().url(url).build()
               chain.proceed(request)
           }
           .addInterceptor(HttpLoggingInterceptor().apply {
               level = HttpLoggingInterceptor.Level.BODY
           })
           .build()

   }

    @Singleton
    @Provides
    fun providesApplicationContext(@ApplicationContext context: Context) = context

    @Singleton
    @Provides
    fun providesClientId(@ApplicationContext context: Context) : String {
        return runBlocking {context.dataStore.clientId() }
    }


    @Singleton
    @Provides
    fun providesSetupRepository(setupApi: SetupApi,@ApplicationContext context: Context) : SetupRepository = DefaultSetupRepositoryImpl(setupApi, context)

    @Singleton
    @Provides
    ////Serialiszed or deserialized json
    fun providesGsonInstance() : Gson{
        return Gson()
    }

    @Singleton
    @Provides
    fun providesSetupApi(okHttpClient: OkHttpClient) : SetupApi{
    return Retrofit.Builder()
        .baseUrl(if (USE_LOCALHOST) HTTP_BASE_URL_LOCAL_HOST else HTTP_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SetupApi::class.java)
    }



    @Provides
    @Singleton
    fun providesCoroutinesDispatcher() : DispatcherProvider{
        return object : DispatcherProvider
        {
            override val main: CoroutineDispatcher
                get() = Dispatchers.Main
            override val io: CoroutineDispatcher
                get() = Dispatchers.IO
            override val default: CoroutineDispatcher
                get() = Dispatchers.Default

        }
    }

}