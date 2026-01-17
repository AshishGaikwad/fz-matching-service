package tech.grastone.fz.matching.exception;

public class DataLimitException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DataLimitException(String message) {
		super(message);
	}

}
