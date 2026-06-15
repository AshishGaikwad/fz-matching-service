package tech.grastone.fz.matching.dto;

import lombok.Data;

@Data
public class VibeRequestReplyDto {
    private Long requestId;
    private Long userId;
    private String responseMessage;
}
