package com.a307.linkcare.feature.mypage.domain.repository

import com.a307.linkcare.feature.mypage.data.api.ShopApi
import com.a307.linkcare.feature.mypage.data.model.dto.ShopBackgroundResponse
import com.a307.linkcare.feature.mypage.data.model.dto.ShopCharacterResponse
import javax.inject.Inject

class ShopRepository @Inject constructor(
    private val api: ShopApi
) {

    suspend fun loadCharacters(): ShopCharacterResponse =
        api.getCharacters()

    suspend fun loadBackgrounds(): ShopBackgroundResponse =
        api.getBackgrounds()

    suspend fun buyCharacter(id: Long) =
        api.buyCharacter(id)

    suspend fun buyBackground(id: Long) =
        api.buyBackground(id)
}
