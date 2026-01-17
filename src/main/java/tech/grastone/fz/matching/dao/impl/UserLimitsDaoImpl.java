package tech.grastone.fz.matching.dao.impl;

import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.UserLimitsDao;
import tech.grastone.fz.matching.entity.UserLimitsEntity;
import tech.grastone.fz.matching.enums.Frequency;
import tech.grastone.fz.matching.enums.LimitType;
import tech.grastone.fz.matching.repository.UserLimitsRepository;

@Repository
@AllArgsConstructor
public class UserLimitsDaoImpl implements UserLimitsDao{

	private final UserLimitsRepository limitsRepository;
	
	@Override
	public UserLimitsEntity getUserLimits(long userId, Frequency frequency, LimitType limitType, String periodKey) {
		return limitsRepository.findByUserIdAndFrequencyAndLimitTypeAndPeriodKey(userId, frequency, limitType, periodKey);
	}

	@Override
	public UserLimitsEntity save(UserLimitsEntity userLimitsEntity) {
		return limitsRepository.saveAndFlush(userLimitsEntity);
	}

}
