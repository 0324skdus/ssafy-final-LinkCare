package com.ssafy.linkcare.character.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id")
    private Long characterId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int price = 0;

    @Column(name = "base_image_s3_key")
    private String baseImageS3Key;

    @Column(name = "animated_image_s3_key")
    private String animatedImageS3Key;
}
