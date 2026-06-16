package tech.grastone.fz.matching.dto;

import lombok.Data;
import tech.grastone.fz.matching.enums.Drinking;
import tech.grastone.fz.matching.enums.Lifestyle;
import tech.grastone.fz.matching.enums.LookingFor;
import tech.grastone.fz.matching.enums.MaritalStatus;
import tech.grastone.fz.matching.enums.Personality;
import tech.grastone.fz.matching.enums.Religion;
import tech.grastone.fz.matching.enums.Smoking;

import java.util.Set;

@Data
public class PreferencesDto {
	
	private long userId;
	
	private int minAge;
	
	private int maxAge;
	
	private int distance;
	
	private Smoking smoking; 
	
	private Drinking drinking;
	
	private Personality personality;
	
	private Religion religion;
	
	private Lifestyle lifestyle;

	private MaritalStatus maritalStatus;

	private String profession;

	private Set<LookingFor> lookingFor;


}
