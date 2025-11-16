package com.ssafy.sdk.health.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ssafy.sdk.health.data.adapter.InstantAdapter
import com.ssafy.sdk.health.data.adapter.LocalDateTimeAdapter
import com.ssafy.sdk.health.data.adapter.ZoneOffsetAdapter
import com.ssafy.sdk.health.data.remote.HealthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://192.168.0.236:9090/"

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            // LocalDateTime 직렬화
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            // Instant 직렬화
            .registerTypeAdapter(Instant::class.java, InstantAdapter())
            // ZoneOffset 직렬화
            .registerTypeAdapter(ZoneOffset::class.java, ZoneOffsetAdapter())
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideHealthApiService(retrofit: Retrofit): HealthApiService {
        return retrofit.create(HealthApiService::class.java)
    }
}
