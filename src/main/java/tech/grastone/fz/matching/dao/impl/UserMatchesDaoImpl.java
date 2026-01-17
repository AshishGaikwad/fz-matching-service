package tech.grastone.fz.matching.dao.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dao.UserMatchesDao;
import tech.grastone.fz.matching.entity.UserMatchesEntity;
import tech.grastone.fz.matching.enums.MatchStatus;
import tech.grastone.fz.matching.enums.MatchType;
import tech.grastone.fz.matching.exception.DataNotFoundException;
import tech.grastone.fz.matching.repository.UserMatchesRepository;

@Repository
@AllArgsConstructor
@Slf4j
public class UserMatchesDaoImpl implements UserMatchesDao {

	private final UserMatchesRepository userMatchesRepository;

	@Override
	public Page<UserMatchesEntity> findByUserIdAndStatusAndType(Long uid, MatchStatus matchStatus, MatchType matchType,
			Pageable pageable) {
		if (uid == null || matchStatus == null || matchType == null) {
			log.warn("Invalid input for finding matches. uid: {}, status: {}, type: {}", uid, matchStatus, matchType);
			return Page.empty();
		}
		return userMatchesRepository.findByUserId1AndMatchStatusAndMatchType(uid, matchStatus, matchType, pageable);
	}

	@Override
	public List<UserMatchesEntity> saveAll(List<UserMatchesEntity> userMatchesEntities) {
		if (userMatchesEntities == null || userMatchesEntities.isEmpty()) {
			log.warn("Attempted to save empty or null list of UserMatchesEntities");
			return Collections.emptyList();
		}
		return userMatchesRepository.saveAllAndFlush(userMatchesEntities);
	}

	@Override
	public UserMatchesEntity getById(Long userMatchesId) {
		if (userMatchesId == null) {
			log.error("Null userMatchesId provided to getById()");
			throw new IllegalArgumentException("userMatchesId cannot be null");
		}
		Optional<UserMatchesEntity> entity = userMatchesRepository.findById(userMatchesId);
		return entity.orElseThrow(() -> {
			log.warn("UserMatchesEntity not found for ID: {}", userMatchesId);
			return new DataNotFoundException("User match not found for ID: " + userMatchesId);
		});
	}

	@Override
	public UserMatchesEntity save(UserMatchesEntity userMatchesEntity) {
		if (userMatchesEntity == null) {
			log.error("Null UserMatchesEntity passed to save()");
			throw new IllegalArgumentException("Cannot save null entity");
		}
		return userMatchesRepository.saveAndFlush(userMatchesEntity);
	}
}
