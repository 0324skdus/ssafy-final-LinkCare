package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.util.List;

@Data
public class WaterIntakeDto {
    private List<WaterIntakeGroupedDto> waterIntakes;
    private float goal;
}
