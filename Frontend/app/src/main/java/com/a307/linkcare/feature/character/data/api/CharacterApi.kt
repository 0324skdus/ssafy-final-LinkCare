package com.a307.linkcare.feature.character.data.api

import com.a307.linkcare.feature.character.data.model.dto.CharacterStatusDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CharacterApi {

    @POST("api/characters/select-initial/{characterId}") // 초기 캐릭터 선택
    suspend fun selectInitial(@Path("characterId") characterId: Long): Response<Unit>

    @GET("api/characters") // 전체 캐릭터 조회
    suspend fun getAll(): List<CharacterStatusDto>

    @GET("api/characters/main") // 메인 캐릭터 조회
    suspend fun getMain(): Response<CharacterStatusDto>

    @POST("api/characters/main/{userCharacterId}") // 메인 캐릭터 변경
    suspend fun setMain(@Path("userCharacterId") userCharacterId: Long): Response<Unit>

    @POST("api/characters/unlock/{characterId}") // 캐릭터 해금
    suspend fun unlock(@Path("characterId") characterId: Long): Response<Unit>
}