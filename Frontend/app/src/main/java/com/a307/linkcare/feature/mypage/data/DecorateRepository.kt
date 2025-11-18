package com.a307.linkcare.feature.mypage.data

import javax.inject.Inject

class DecorateRepository @Inject constructor(
    private val api: DecorateApi
) {

    suspend fun getCharacters() = api.getCharacters()

    suspend fun getMainCharacter() = api.getMainCharacter()

    suspend fun unlockCharacter(characterId: Long) =
        api.unlockCharacter(characterId)

    suspend fun setMainCharacter(userCharacterId: Long) =
        api.setMainCharacter(userCharacterId)

    suspend fun getBackgrounds() = api.getBackgrounds()

    suspend fun getMainBackground() = api.getMainBackground()

    suspend fun unlockBackground(backgroundId: Long) =
        api.unlockBackground(backgroundId)

    suspend fun setMainBackground(userBackgroundId: Long) =
        api.setMainBackground(userBackgroundId)
}
