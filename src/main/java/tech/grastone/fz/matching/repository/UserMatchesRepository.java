package tech.grastone.fz.matching.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import tech.grastone.fz.matching.entity.UserMatchesEntity;
import tech.grastone.fz.matching.enums.MatchStatus;
import tech.grastone.fz.matching.enums.MatchType;

public interface UserMatchesRepository extends JpaRepository<UserMatchesEntity, Long> {

	List<UserMatchesEntity> findByUserId1AndMatchStatusAndMatchType(long userId1, MatchStatus matchStatus,
			MatchType matchType);

	Page<UserMatchesEntity> findByUserId1AndMatchStatusAndMatchType(long userId1, MatchStatus matchStatus,
			MatchType matchType, Pageable pageable);
}
