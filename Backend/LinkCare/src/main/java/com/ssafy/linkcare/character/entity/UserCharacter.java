package com.ssafy.linkcare.character.entity;

import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_character_id")
    private Long userCharacterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pk", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Builder
    public UserCharacter(User user, Character character) {
        this.user = user;
        this.character = character;
    }
}
