package tech.grastone.fz.matching.dto;

import lombok.Data;

@Data
public class LowkeyRequestDto {
    private Long senderId;
    private Long receiverId;
    private Long sessionId;
    private String requestMessage;
}
