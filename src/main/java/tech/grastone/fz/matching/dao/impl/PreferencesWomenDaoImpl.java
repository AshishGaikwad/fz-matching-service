package tech.grastone.fz.matching.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dao.PreferencesDao;
import tech.grastone.fz.matching.dto.MatchedByPreferencesDto;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.PreferencesWomenEntity;
import tech.grastone.fz.matching.entity.base.BasePreferenceEntity;
import tech.grastone.fz.matching.repository.PreferencesWomenRepository;
import tech.grastone.fz.matching.util.QueryUtil;

@Slf4j
@Repository
@AllArgsConstructor
public class PreferencesWomenDaoImpl implements PreferencesDao {
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private final PreferencesWomenRepository preferencesWomenRepository;

	private final QueryUtil queryUtil;
	
	@Override
	public Object save(BasePreferenceEntity preferenceEntity) {
		return preferencesWomenRepository.saveAndFlush((PreferencesWomenEntity) preferenceEntity);
	}

	@Override
	public Optional<?> get(long userId) {
		return preferencesWomenRepository.findById(userId);
	}

}
