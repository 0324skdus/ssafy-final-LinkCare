package com.ssafy.linkcare.shop.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ShopCharacterDto {
    private Long characterId;
    private String name;
    private String description;
    private List<String> imageUrls;
    private int price;
    private boolean isUnlocked;
}
