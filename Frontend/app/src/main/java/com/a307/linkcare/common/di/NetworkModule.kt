//package com.a307.linkcare.common.di
//
//import com.a307.linkcare.feature.auth.data.AuthApi
//import okhttp3.OkHttpClient
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
//object NetworkModule {
//    private val client by lazy {
//        OkHttpClient.Builder().build()
//    }
//
//    private val retrofit by lazy {
//        Retrofit.Builder()
//            .baseUrl("http://k13a307.p.ssafy.io/api/")
//            .client(client)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
//}
