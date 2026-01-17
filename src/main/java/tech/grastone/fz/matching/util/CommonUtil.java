package tech.grastone.fz.matching.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import tech.grastone.fz.matching.enums.Frequency;

@Component
public class CommonUtil {

	public String getPeriod(Frequency frequency) {

		LocalDate toDay = LocalDate.now();

		return switch (frequency) {
		case DAILY -> DateTimeFormatter.ofPattern("yyyy_MM_dd").format(toDay);
		case MONTHLY -> DateTimeFormatter.ofPattern("yyyy_MM").format(toDay);
		default -> throw new IllegalArgumentException("Unexpected value: " + frequency);
		};
	}
}
