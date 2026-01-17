package tech.grastone.fz.matching.dto;

import lombok.Data;

@Data
public class SendMatchRequestDto {
	private Long senderId;
	private Long receiverId;
	private String requestMessage;
}
