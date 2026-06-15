package tech.grastone.fz.matching.dto;

import lombok.Data;

@Data
public class JoinVibeRequestDto {
    private Long userId;
    private Long vibeId;
    private Double latitude;
    private Double longitude;
    private Integer radiusKm;
    private Integer durationMinutes;
}
