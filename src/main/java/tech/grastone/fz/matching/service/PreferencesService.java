package tech.grastone.fz.matching.service;

import tech.grastone.fz.matching.dto.PreferencesDto;

public interface PreferencesService {
	public PreferencesDto save(PreferencesDto preferencesDto);
	public PreferencesDto get(int userId);
	
}
