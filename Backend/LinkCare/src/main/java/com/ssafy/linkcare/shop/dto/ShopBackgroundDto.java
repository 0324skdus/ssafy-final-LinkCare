package com.ssafy.linkcare.shop.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopBackgroundDto {
    private Long backgroundId;
    private String name;
    private String description;
    private String imageUrl;
    private int price;
    private boolean isUnlocked;
}
