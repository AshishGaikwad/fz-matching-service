package tech.grastone.fz.matching.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ActiveVibeDto {
    private Long sessionId;
    private VibeDto vibe;
    private boolean joined;
    private long participantCount;
    private long remainingSeconds;
    private LocalDateTime joinedAt;
    private LocalDateTime endsAt;
    private int radiusKm;
}
