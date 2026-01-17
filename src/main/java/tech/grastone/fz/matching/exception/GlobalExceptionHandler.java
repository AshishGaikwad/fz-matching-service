package tech.grastone.fz.matching.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import tech.grastone.fz.matching.handler.ErrorHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(value = ValidationException.class)
	public ResponseEntity<?> validationException(ValidationException ex) {
		return ResponseEntity.ok().body(new ErrorHandler(ex.getLocalizedMessage(), 1));
	}

	@ExceptionHandler(value = DataNotFoundException.class)
	public ResponseEntity<?> dataNotFoundEx(DataNotFoundException ex) {
		return ResponseEntity.ok().body(new ErrorHandler(ex.getLocalizedMessage(), 2));
	}

	@ExceptionHandler(value = DataLimitException.class)
	public ResponseEntity<?> dataLimitEx(DataLimitException ex) {
		return ResponseEntity.ok().body(new ErrorHandler(ex.getLocalizedMessage(), 3));
	}
}
