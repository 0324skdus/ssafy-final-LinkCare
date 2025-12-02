package com.ssafy.linkcare.background.entity;

import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_backgrounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBackground {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_background_id")
    private Long userBackgroundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pk", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id", nullable = false)
    private Background background;

    @Builder
    public UserBackground(User user, Background background) {
        this.user = user;
        this.background = background;
    }
}
