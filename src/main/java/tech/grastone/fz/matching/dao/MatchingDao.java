package tech.grastone.fz.matching.dao;

import java.util.List;

import org.springframework.data.domain.Pageable;

import tech.grastone.fz.matching.dto.MatchedByPreferencesDto;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.MatchRequestEntity;

public interface MatchingDao {
	public List<MatchedByPreferencesDto> getMatchedUserUsingPreferences(UserDto userDto,
			PreferencesDto userPreferencesDto, Pageable pageable);
}
