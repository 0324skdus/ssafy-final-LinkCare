package com.ssafy.linkcare.shop.service;

import com.ssafy.linkcare.character.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final CharacterService characterService;

    @Transactional
    public void buyCharacter(Long userPk, Long characterId) {
        // 상점 관련 로직 (예: 판매중인 캐릭터인지 확인, 할인 적용 등)
        // 현재는 별도 로직이 없으므로 바로 CharacterService 호출

        characterService.unlockCharacter(userPk, characterId);
    }
}
