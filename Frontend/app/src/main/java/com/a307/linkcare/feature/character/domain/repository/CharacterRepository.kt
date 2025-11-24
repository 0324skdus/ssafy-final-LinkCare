package com.a307.linkcare.feature.character.domain.repository

import com.a307.linkcare.feature.character.data.api.CharacterApi
import com.a307.linkcare.feature.character.data.model.dto.CharacterStatusDto
import retrofit2.HttpException
import javax.inject.Inject

class CharacterRepository @Inject constructor(
    private val api: CharacterApi
) {
    suspend fun getCharacters(): List<CharacterStatusDto> =
        api.getAll()

    suspend fun getMain(): CharacterStatusDto? {
        val res = api.getMain()
        return when {
            res.code() == 204 -> null
            res.isSuccessful -> res.body()
            else -> throw HttpException(res)
        }
    }

    suspend fun selectInitial(characterId: Long) {
        val res = api.selectInitial(characterId)
        if (!res.isSuccessful) throw HttpException(res)
    }

    suspend fun setMain(userCharacterId: Long) {
        val res = api.setMain(userCharacterId)
        if (!res.isSuccessful) throw HttpException(res)
    }

    suspend fun unlock(characterId: Long) {
        val res = api.unlock(characterId)
        if (!res.isSuccessful) throw HttpException(res)
    }
}