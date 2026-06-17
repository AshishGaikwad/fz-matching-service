package tech.grastone.fz.matching.dto;

import java.util.List;

import lombok.Data;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.entity.UserImageEntity;
import tech.grastone.fz.matching.entity.UserLimitsEntity;
import tech.grastone.fz.matching.entity.UserMatchesEntity;

@Data
public class ShowProfileDto {
	private String id;
	public UserDto user;
	public PreferencesDto preference;
	public UserMatchesEntity userMatch;
	public UserLimitsEntity userLimits;
	public MatchRequestEntity matchRequests;
	public List<UserImageEntity> userImages;
	public boolean hidden;
	public UserLimitStatusDto limitStatus;

}
