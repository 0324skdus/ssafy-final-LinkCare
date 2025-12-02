package com.a307.linkcare.feature.mypage.data.api

import com.a307.linkcare.feature.mypage.data.model.dto.BackgroundDto
import com.a307.linkcare.feature.mypage.data.model.dto.CharacterDto
import retrofit2.http.*

interface DecorateApi {

    @GET("/api/characters")
    suspend fun getCharacters(): List<CharacterDto>

    @GET("/api/characters/main")
    suspend fun getMainCharacter(): CharacterDto

    @POST("/api/characters/unlock/{characterId}")
    suspend fun unlockCharacter(
        @Path("characterId") characterId: Long
    )

    @POST("/api/characters/main/{characterId}")
    suspend fun setMainCharacter(
        @Path("characterId") characterId: Long
    )

    @GET("/api/backgrounds")
    suspend fun getBackgrounds(): List<BackgroundDto>

    @GET("/api/backgrounds/main")
    suspend fun getMainBackground(): BackgroundDto

    @POST("/api/backgrounds/unlock/{backgroundId}")
    suspend fun unlockBackground(
        @Path("backgroundId") backgroundId: Long
    )

    @POST("/api/backgrounds/main/{backgroundId}")
    suspend fun setMainBackground(
        @Path("backgroundId") backgroundId: Long
    )
}
