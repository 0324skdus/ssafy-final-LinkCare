package com.a307.linkcare.feature.mypage.data.api

import com.a307.linkcare.feature.mypage.data.model.dto.ShopBackgroundResponse
import com.a307.linkcare.feature.mypage.data.model.dto.ShopCharacterResponse
import retrofit2.http.*

interface ShopApi {

    @GET("/api/v1/shop/characters")
    suspend fun getCharacters(): ShopCharacterResponse

    @GET("/api/v1/shop/backgrounds")
    suspend fun getBackgrounds(): ShopBackgroundResponse

    @POST("/api/v1/shop/buy/character/{id}")
    suspend fun buyCharacter(@Path("id") id: Long)

    @POST("/api/v1/shop/buy/background/{id}")
    suspend fun buyBackground(@Path("id") id: Long)
}
