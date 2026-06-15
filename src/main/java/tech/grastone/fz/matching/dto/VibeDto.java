package tech.grastone.fz.matching.dto;

import java.time.LocalDateTime;

import lombok.Data;
import tech.grastone.fz.matching.enums.VibeActivityType;

@Data
public class VibeDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private VibeActivityType activityType;
    private int defaultDurationMinutes;
    private boolean active;
    private Long activeSessionId;
    private long participantCount;
    private long remainingSeconds;
    private LocalDateTime endsAt;
}
