package tech.grastone.fz.matching.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.enums.Drinking;
import tech.grastone.fz.matching.enums.Lifestyle;
import tech.grastone.fz.matching.enums.MaritalStatus;
import tech.grastone.fz.matching.enums.Personality;
import tech.grastone.fz.matching.enums.Religion;
import tech.grastone.fz.matching.enums.Smoking;

@MappedSuperclass
@Getter
@Setter
public class BasePreferenceEntity extends BaseEntity{

	@Id
	@Column(unique = true)
	private long userId;
	
	private int minAge;
	
	private int maxAge;
	
	private int distance;
	
	@Enumerated(EnumType.ORDINAL)
	private Smoking smoking; 
	
	@Enumerated(EnumType.ORDINAL)
	private Drinking drinking;
	

	@Enumerated(EnumType.ORDINAL)
	private Personality personality;
	
	@Enumerated(EnumType.ORDINAL)
	private Religion religion;
	
	@Enumerated(EnumType.ORDINAL)
	private Lifestyle lifestyle;

	@Enumerated(EnumType.STRING)
	@Column(name = "marital_status", length = 24)
	private MaritalStatus maritalStatus;

	@Column(length = 80)
	private String profession;

	@Column(name = "looking_for", length = 256)
	private String lookingForValues;

}
