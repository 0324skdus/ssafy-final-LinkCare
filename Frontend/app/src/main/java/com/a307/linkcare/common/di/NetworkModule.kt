package com.a307.linkcare.common.di

import android.content.Context
import com.a307.linkcare.feature.auth.data.*
import com.a307.linkcare.feature.character.data.CharacterApi
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
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
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

}
