package com.ssafy.linkcare.point.entity;

import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pk", nullable = false)
    private User user;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int balance = 0;

    public Point(User user) {
        this.user = user;
    }

    public void add(int amount) {
        if (amount <= 0) return;
        this.balance += amount;
    }

    public void use(int amount) {
        if (amount <= 0) return;
        if (this.balance < amount) {
            // 이 예외는 서비스 레이어에서 처리하는 것이 더 좋습니다.
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.balance -= amount;
    }
}
