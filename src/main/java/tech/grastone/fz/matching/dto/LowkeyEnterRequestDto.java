package tech.grastone.fz.matching.dto;

import java.util.Set;

import lombok.Data;
import tech.grastone.fz.matching.enums.LookingFor;

@Data
public class LowkeyEnterRequestDto {
    private Long userId;
    private Double latitude;
    private Double longitude;
    private Integer locationAccuracyMeters;
    private Integer radiusKm;
    private Integer durationMinutes;
    private Set<LookingFor> lookingFor;
}
