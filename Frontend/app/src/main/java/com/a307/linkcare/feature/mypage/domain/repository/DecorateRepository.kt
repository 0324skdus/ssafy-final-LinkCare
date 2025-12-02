package com.a307.linkcare.feature.mypage.domain.repository

import com.a307.linkcare.feature.mypage.data.api.DecorateApi
import javax.inject.Inject

class DecorateRepository @Inject constructor(
    private val api: DecorateApi
) {

    suspend fun getCharacters() = api.getCharacters()

    suspend fun getMainCharacter() = api.getMainCharacter()

    suspend fun unlockCharacter(characterId: Long) =
        api.unlockCharacter(characterId)

    suspend fun setMainCharacter(characterId: Long) =
        api.setMainCharacter(characterId)

    suspend fun getBackgrounds() = api.getBackgrounds()

    suspend fun getMainBackground() = api.getMainBackground()

    suspend fun unlockBackground(backgroundId: Long) =
        api.unlockBackground(backgroundId)

    suspend fun setMainBackground(backgroundId: Long) =
        api.setMainBackground(backgroundId)
}
