package tech.grastone.fz.matching.dao;

import java.util.List;
import java.util.Optional;

import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.base.BasePreferenceEntity;

public interface PreferencesDao {
	Object save(BasePreferenceEntity preferenceEntity);
	Optional<?> get(long userId);
	
}
