package com.a307.linkcare.feature.character.data

import com.a307.linkcare.feature.character.domain.CharacterStatusDto
import retrofit2.Response
import retrofit2.http.*

interface CharacterApi {

    @POST("api/characters/select-initial/{characterId}")
    suspend fun selectInitial(@Path("characterId") characterId: Long): Response<Unit>

    @GET("api/characters")
    suspend fun getAll(): List<CharacterStatusDto>

    @GET("api/characters/main")
    suspend fun getMain(): Response<CharacterStatusDto>

    @POST("api/characters/main/{userCharacterId}")
    suspend fun setMain(@Path("userCharacterId") userCharacterId: Long): Response<Unit>

    @POST("api/characters/unlock/{characterId}")
    suspend fun unlock(@Path("characterId") characterId: Long): Response<Unit>
}