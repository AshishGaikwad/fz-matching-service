package tech.grastone.fz.matching.handler;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorHandler {
	private String errorMsg;
	private int errorCode;

}
