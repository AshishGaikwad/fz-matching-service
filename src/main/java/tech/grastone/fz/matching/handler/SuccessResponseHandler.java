package tech.grastone.fz.matching.handler;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponseHandler<T> {

	private int status;
	private String message;
	private T body;

	private Integer page;
	private Integer size;
	private Long totalElements;
	private Integer totalPages;
	private Boolean last;

	public SuccessResponseHandler(int status, String message, T body) {
		this.status = status;
		this.message = message;
		this.body = body;
	}


	public SuccessResponseHandler(int status, String message, Page<?> page) {
		this.status = status;
		this.message = message;
		this.body = (T) page.getContent();
		this.page = page.getNumber();
		this.size = page.getSize();
		this.totalElements = page.getTotalElements();
		this.totalPages = page.getTotalPages();
		this.last = page.isLast();
	}
}
