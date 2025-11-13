package com.ssafy.linkcare.character.repository;

import com.ssafy.linkcare.character.entity.UserCharacter;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {
    List<UserCharacter> findByUser(User user);
    Optional<UserCharacter> findByUserAndUserCharacterId(User user, Long userCharacterId);
}
