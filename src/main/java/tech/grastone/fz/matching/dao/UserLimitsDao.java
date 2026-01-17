package tech.grastone.fz.matching.dao;

import tech.grastone.fz.matching.entity.UserLimitsEntity;
import tech.grastone.fz.matching.enums.Frequency;
import tech.grastone.fz.matching.enums.LimitType;

public interface UserLimitsDao {

	public UserLimitsEntity getUserLimits(long userId, Frequency frequency, LimitType limitType, String periodKey);

	public UserLimitsEntity save(UserLimitsEntity userLimitsEntity);
}
