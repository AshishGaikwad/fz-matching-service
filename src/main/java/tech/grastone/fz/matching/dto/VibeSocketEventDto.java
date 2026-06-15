package tech.grastone.fz.matching.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VibeSocketEventDto {
    private String type;
    private Long vibeId;
    private Long sessionId;
    private Long userId;
    private Long targetUserId;
    private long participantCount;
    private long remainingSeconds;
    private Map<String, Object> payload;
}
