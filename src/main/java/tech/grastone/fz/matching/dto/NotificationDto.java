package tech.grastone.fz.matching.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {
    private long toUserId;
    private String notificationTitle;
    private String notificationMessage;
}
