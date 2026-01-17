package tech.grastone.fz.matching.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.PreferencesDao;
import tech.grastone.fz.matching.entity.PreferencesGayEntity;
import tech.grastone.fz.matching.entity.base.BasePreferenceEntity;
import tech.grastone.fz.matching.repository.PreferencesGayRepository;

@AllArgsConstructor
@Repository
public class PreferencesGayDaoImpl implements PreferencesDao {

	private final PreferencesGayRepository preferencesGayRepository;

	@Override
	public Object save(BasePreferenceEntity preferenceEntity) {
		return preferencesGayRepository.saveAndFlush((PreferencesGayEntity) preferenceEntity);
	}

	@Override
	public Optional<?> get(long userId) {
		return preferencesGayRepository.findById(userId);
	}

}
