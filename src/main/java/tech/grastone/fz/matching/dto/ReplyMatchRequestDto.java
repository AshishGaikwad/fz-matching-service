package tech.grastone.fz.matching.dto;

import lombok.Data;
import tech.grastone.fz.matching.enums.RequestStatus;

@Data
public class ReplyMatchRequestDto {
	private Long id;
	private String replyMessage;
	private RequestStatus requestStatus;
}
