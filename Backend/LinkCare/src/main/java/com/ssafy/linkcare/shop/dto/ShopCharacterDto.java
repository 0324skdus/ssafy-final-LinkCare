package com.ssafy.linkcare.shop.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopCharacterDto {
    private Long characterId;
    private String name;
    private String description;
    private String baseImageUrl;
    private String animatedImageUrl;
    private int price;
    private boolean isUnlocked;
}
