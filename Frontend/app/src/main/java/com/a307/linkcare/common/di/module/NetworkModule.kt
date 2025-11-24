package com.a307.linkcare.common.di.module

import android.content.Context
import com.a307.linkcare.common.network.interceptor.AuthInterceptor
import com.a307.linkcare.common.network.interceptor.TokenAuthenticator
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.ai.data.api.AiApi
import com.a307.linkcare.feature.auth.data.api.AuthApi
import com.a307.linkcare.feature.auth.data.api.ProfileApi
import com.a307.linkcare.feature.character.data.api.CharacterApi
import com.a307.linkcare.feature.mypage.data.api.DecorateApi
import com.a307.linkcare.feature.mypage.domain.repository.DecorateRepository
import com.a307.linkcare.feature.mypage.data.api.ShopApi
import com.a307.linkcare.feature.mypage.domain.repository.ShopRepository
import com.a307.linkcare.feature.healthgroup.data.api.HealthSyncApi
import com.a307.linkcare.feature.notification.data.api.NotificationApi
import com.a307.linkcare.feature.watch.data.api.ExerciseSessionApi
import com.a307.linkcare.feature.workout.data.api.WorkoutApi
import com.a307.linkcare.sdk.health.data.remote.HealthApiService
import com.a307.linkcare.sdk.health.data.serializer.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.samsung.android.sdk.health.data.device.DeviceType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://k13a307.p.ssafy.io:9090/"

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore =
        TokenStore(context) // :contentReference[oaicite:4]{index=4}

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: TokenStore): AuthInterceptor =
        AuthInterceptor(tokenStore) // :contentReference[oaicite:5]{index=5}

    @Provides
    @Singleton
    fun provideTokenAuthenticator(@ApplicationContext context: Context): TokenAuthenticator =
        TokenAuthenticator(context, BASE_URL.toHttpUrl()) // :contentReference[oaicite:6]{index=6}

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            )
            .addInterceptor(authInterceptor)      // 액세스 토큰 헤더 주입
            .authenticator(tokenAuthenticator)    // 401 → refresh 재시도
            .build()

    @Provides
    @Singleton
    fun provideGson(): Gson =
        GsonBuilder()
            .registerTypeAdapter(DeviceType::class.java, DeviceTypeSerializer())
            .registerTypeAdapter(ZoneOffset::class.java, ZoneOffsetSerializer())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
            .create()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi =
        retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideCharacterApi(retrofit: Retrofit): CharacterApi =
        retrofit.create(CharacterApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideHealthApiService(retrofit: Retrofit): HealthApiService =
        retrofit.create(HealthApiService::class.java)
    @Provides
    @Singleton
    fun provideWorkoutApi(retrofit: Retrofit): WorkoutApi =
        retrofit.create(WorkoutApi::class.java)

    @Provides
    @Singleton
    fun provideHealthSyncApi(retrofit: Retrofit): HealthSyncApi =
        retrofit.create(HealthSyncApi::class.java)

    @Provides
    @Singleton
    fun provideAiApi(retrofit: Retrofit): AiApi =
        retrofit.create(AiApi::class.java)

    @Provides
    @Singleton
    fun provideShopApi(retrofit: Retrofit): ShopApi {
        return retrofit.create(ShopApi::class.java)
    }

    @Provides
    @Singleton
    fun provideShopRepository(
        api: ShopApi
    ): ShopRepository {
        return ShopRepository(api)
    }

    @Provides
    @Singleton
    fun provideDecorateApi(retrofit: Retrofit): DecorateApi =
        retrofit.create(DecorateApi::class.java)

    @Provides
    @Singleton
    fun provideDecorateRepository(api: DecorateApi): DecorateRepository {
        return DecorateRepository(api)
    }

    @Provides
    @Singleton
    fun provideExerciseSessionApi(retrofit: Retrofit): ExerciseSessionApi =
        retrofit.create(ExerciseSessionApi::class.java)
}
