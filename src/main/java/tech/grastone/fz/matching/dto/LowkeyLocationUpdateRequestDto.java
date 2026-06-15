package tech.grastone.fz.matching.dto;

import lombok.Data;

@Data
public class LowkeyLocationUpdateRequestDto {
    private Long userId;
    private Long sessionId;
    private Double latitude;
    private Double longitude;
    private Integer locationAccuracyMeters;
}
