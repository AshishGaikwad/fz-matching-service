package tech.grastone.fz.matching.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.PreferencesDao;
import tech.grastone.fz.matching.entity.PreferencesLesbianEntity;
import tech.grastone.fz.matching.entity.base.BasePreferenceEntity;
import tech.grastone.fz.matching.repository.PreferencesLesbianRepository;

@AllArgsConstructor
@Repository
public class PreferencesLesbianDaoImpl implements PreferencesDao{
	private final PreferencesLesbianRepository preferencesLesbianRepository;

	@Override
	public Object save(BasePreferenceEntity preferenceEntity) {
		return preferencesLesbianRepository.saveAndFlush((PreferencesLesbianEntity) preferenceEntity);
	}

	@Override
	public Optional<?> get(long userId) {
		return preferencesLesbianRepository.findById(userId);
	}

}
