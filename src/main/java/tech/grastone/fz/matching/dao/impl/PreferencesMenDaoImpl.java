package tech.grastone.fz.matching.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.PreferencesDao;
import tech.grastone.fz.matching.dto.MatchedByPreferencesDto;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.PreferencesMenEntity;
import tech.grastone.fz.matching.entity.base.BasePreferenceEntity;
import tech.grastone.fz.matching.repository.PreferencesMenRepository;
import tech.grastone.fz.matching.repository.PreferencesWomenRepository;
import tech.grastone.fz.matching.util.QueryUtil;

@Repository
@AllArgsConstructor
public class PreferencesMenDaoImpl implements PreferencesDao {

	private final PreferencesMenRepository preferencesMenRepository;


	@Override
	public Object save(BasePreferenceEntity preferenceEntity) {
		return preferencesMenRepository.saveAndFlush((PreferencesMenEntity) preferenceEntity);
	}

	@Override
	public Optional<?> get(long userId) {
		return preferencesMenRepository.findById(userId);
	}

	

}
