package com.ssafy.linkcare.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopCharacterResponseDto {
    private int userPoints;
    private List<ShopCharacterDto> characters;
}
