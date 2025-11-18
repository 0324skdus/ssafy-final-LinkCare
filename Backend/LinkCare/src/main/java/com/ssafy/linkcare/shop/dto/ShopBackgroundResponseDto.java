package com.ssafy.linkcare.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopBackgroundResponseDto {
    private int userPoints;
    private List<ShopBackgroundDto> backgrounds;

}
