package tech.grastone.fz.matching.dto;

import lombok.Data;

@Data
public class VibeRequestDto {
    private Long senderId;
    private Long receiverId;
    private Long vibeId;
    private Long sessionId;
    private String requestMessage;
}
