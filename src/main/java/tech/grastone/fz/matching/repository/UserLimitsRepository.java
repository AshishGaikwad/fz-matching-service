package tech.grastone.fz.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.UserLimitsEntity;
import tech.grastone.fz.matching.enums.Frequency;
import tech.grastone.fz.matching.enums.LimitType;


@Repository
public interface UserLimitsRepository extends JpaRepository<UserLimitsEntity, Long>{
	public UserLimitsEntity findByUserIdAndFrequencyAndLimitTypeAndPeriodKey(long userId, Frequency frequency, LimitType limitType, String periodKey);
}
