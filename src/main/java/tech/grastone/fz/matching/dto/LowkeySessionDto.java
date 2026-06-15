package tech.grastone.fz.matching.dto;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.Data;
import tech.grastone.fz.matching.enums.LookingFor;

@Data
public class LowkeySessionDto {
    private Long sessionId;
    private boolean active;
    private Integer radiusKm;
    private Integer durationMinutes;
    private Double latitude;
    private Double longitude;
    private Integer locationAccuracyMeters;
    private LocalDateTime enteredAt;
    private LocalDateTime expiresAt;
    private long remainingSeconds;
    private long participantCount;
    private Set<LookingFor> lookingFor;
}
