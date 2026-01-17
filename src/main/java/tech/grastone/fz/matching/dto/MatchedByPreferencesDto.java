package tech.grastone.fz.matching.dto;

import lombok.Data;

@Data
public class MatchedByPreferencesDto {

	private long user_id;
	private double distance_km;
	private double matching_per;
}
