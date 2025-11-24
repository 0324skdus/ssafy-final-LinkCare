package com.a307.linkcare.common.network.client

import android.content.Context
import com.a307.linkcare.common.network.interceptor.AuthInterceptor
import com.a307.linkcare.common.network.interceptor.TokenAuthenticator
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.auth.data.api.AuthApi
import com.a307.linkcare.feature.auth.data.api.UserApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://k13a307.p.ssafy.io:9090/"

    fun createOkHttpClient(context: Context): OkHttpClient {
        val tokenStore = TokenStore(context)


        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(
                TokenAuthenticator(
                    appContext = context,
                    baseUrl = BASE_URL.toHttpUrl()
                )
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient(context))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        authApi = retrofit.create(AuthApi::class.java)
        userApi = retrofit.create(UserApi::class.java)
    }

    lateinit var authApi: AuthApi
    lateinit var userApi: UserApi
}