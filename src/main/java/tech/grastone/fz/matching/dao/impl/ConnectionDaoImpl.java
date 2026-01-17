package tech.grastone.fz.matching.dao.impl;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.ConnectionDao;
import tech.grastone.fz.matching.entity.ConnectionsEntity;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.repository.ConnectionRepository;

@Repository
@AllArgsConstructor
public class ConnectionDaoImpl implements ConnectionDao {

	private static final Logger log = LoggerFactory.getLogger(ConnectionDaoImpl.class);

	private final ConnectionRepository connectionRepository;

	@Override
	public ConnectionsEntity save(ConnectionsEntity connectionsEntity) {
		if (connectionsEntity == null) {
			throw new ValidationException("Cannot save a null connection entity.");
		}

		if (connectionsEntity.getUserId1() == null || connectionsEntity.getUserId2() == null) {
			throw new ValidationException("Both userId1 and userId2 must be provided.");
		}

		log.info("Saving connection: userId1={}, userId2={}", connectionsEntity.getUserId1(),
				connectionsEntity.getUserId2());

		ConnectionsEntity savedEntity = connectionRepository.saveAndFlush(connectionsEntity);

		log.info("Connection saved successfully with ID: {}", savedEntity.getId());
		return savedEntity;
	}

	@Override
	public List<ConnectionsEntity> getConnectionByUserId1AndUserId2(Long userId1, Long userId2) {
		if (userId1 == null || userId2 == null) {
			log.warn("User IDs must not be null. userId1: {}, userId2: {}", userId1, userId2);
			throw new ValidationException("Both user IDs are required to fetch connections.");
		}

		// Normalize order to ensure consistency (userId1 < userId2)
		Long normalizedUserId1 = Math.min(userId1, userId2);
		Long normalizedUserId2 = Math.max(userId1, userId2);

		log.info("Fetching connection between user {} and user {}", normalizedUserId1, normalizedUserId2);

		List<ConnectionsEntity> connections = connectionRepository.findByUserId1AndUserId2(normalizedUserId1,
				normalizedUserId2);

		log.info("Found {} connection(s) between user {} and user {}", connections.size(), normalizedUserId1,
				normalizedUserId2);

		return connections;
	}

	@Override
	public List<ConnectionsEntity> getConnection(Long userId) {
		log.debug("Fetching connections for userId: {}", userId);

		if (userId == null) {
			log.warn("User ID is null. Returning empty connection list.");
			return Collections.emptyList();
		}

		List<ConnectionsEntity> connections = connectionRepository.findByUserId1OrUserId2(userId, userId);

		log.info("Found {} connections for userId: {}", connections.size(), userId);
		return connections;
	}


}
