package tech.grastone.fz.matching.dao.impl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.MatchRequestDao;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.enums.RequestStatus;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.repository.MatchRequestRepository;

@Repository
@AllArgsConstructor
public class MatchRequestDaoImpl implements MatchRequestDao {

	private static final Logger log = LoggerFactory.getLogger(MatchRequestDaoImpl.class);
	private final MatchRequestRepository matchRequestRepository;

	@Override
	public MatchRequestEntity save(MatchRequestEntity matchRequestEntity) {
		if (matchRequestEntity == null) {
			throw new ValidationException("Cannot save a null match request entity");
		}

		log.info("Saving match request: senderId={}, receiverId={}", matchRequestEntity.getSenderId(),
				matchRequestEntity.getReceiverId());

		MatchRequestEntity savedEntity = matchRequestRepository.saveAndFlush(matchRequestEntity);

		log.info("Match request saved with ID: {}", savedEntity.getMatchRequestId());

		return savedEntity;
	}

	@Override
	public Optional<MatchRequestEntity> get(Long id) {
		return matchRequestRepository.findById(id);
	}

	@Override
	public List<MatchRequestEntity> findBySenderIdAndReceiverId(Long senderId, Long receiverId) {
		return matchRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId);
	}

	@Override
	public Page<MatchRequestEntity> findBySenderIdAndRequestStatus(Long senderId, RequestStatus requestStatus, Pageable pageable) {
		if (senderId == null || senderId <= 0) {
			throw new ValidationException("Invalid senderId: " + senderId);
		}
		if (requestStatus == null) {
			throw new ValidationException("RequestStatus must not be null");
		}
		if (pageable == null) {
			throw new ValidationException("Pageable must not be null");
		}

		log.debug("Fetching sent requests for senderId={}, status={}, page={}",
				senderId, requestStatus.name(), pageable.getPageNumber());

		Page<MatchRequestEntity> result = matchRequestRepository
				.findBySenderIdAndRequestStatus(senderId, requestStatus, pageable);

		log.debug("Fetched {} match requests", result.getNumberOfElements());

		return result;
	}


	@Override
	public Page<MatchRequestEntity> findByReceiverIdAndRequestStatus(Long receiverId, RequestStatus requestStatus, Pageable pageable) {
		if (receiverId == null || receiverId <= 0) {
			throw new ValidationException("Invalid receiverId: " + receiverId);
		}
		if (requestStatus == null) {
			throw new ValidationException("RequestStatus must not be null");
		}
		if (pageable == null) {
			throw new ValidationException("Pageable must not be null");
		}

		log.debug("Fetching sent requests for senderId={}, status={}, page={}",
				receiverId, requestStatus.name(), pageable.getPageNumber());

		Page<MatchRequestEntity> result = matchRequestRepository
				.findByReceiverIdAndRequestStatus(receiverId, requestStatus, pageable);

		log.debug("Fetched {} match requests", result.getNumberOfElements());

		return result;
	}
}
