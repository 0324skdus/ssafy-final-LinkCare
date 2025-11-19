package com.ssafy.linkcare.background.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackgroundStatusDto {
    private Long backgroundId;
    private String name;
    private String description;
    private String imageUrl;
    private boolean isUnlocked;
    private boolean isMain;
}
