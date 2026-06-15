package tech.grastone.fz.matching.dto;

import java.util.List;

import lombok.Data;
import tech.grastone.fz.matching.entity.UserImageEntity;

@Data
public class VibeDiscoverDto {
    private String id;
    private Long sessionId;
    private VibeDto vibe;
    private UserDto user;
    private List<UserImageEntity> userImages;
    private double distanceKm;
    private List<String> badges;
    private int compatibilityPercentage;
    private String requestStatus;
}
