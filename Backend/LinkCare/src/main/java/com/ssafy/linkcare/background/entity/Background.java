package com.ssafy.linkcare.background.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "backgrounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Background {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "background_id")
    private Long backgroundId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int price = 0;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;
}
