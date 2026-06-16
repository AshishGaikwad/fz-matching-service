package tech.grastone.fz.matching.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tech.grastone.fz.matching.dao.*;
import tech.grastone.fz.matching.dto.ShowProfileDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.ConnectionsEntity;
import tech.grastone.fz.matching.entity.UserImageEntity;
import tech.grastone.fz.matching.exception.DataNotFoundException;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.ConnectionService;
import tech.grastone.fz.matching.service.PreferencesService;
import tech.grastone.fz.matching.service.SafetyService;
import tech.grastone.fz.matching.service.client.UserFeingClient;
import tech.grastone.fz.matching.util.CommonUtil;

import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class ConnectionServiceImpl implements ConnectionService {

    private final UserFeingClient userFeingClient;
    private final PreferencesService preferencesService;
    private final MatchingDao matchingDao;
    private final UserLimitsDao limitsDao;
    private final CommonUtil commonUtil;
    private final UserMatchesDao userMatchesDao;
    private final MatchRequestDao matchRequestDao;
    private final ConnectionDao connectionDao;
    private final SafetyService safetyService;

    // In-memory caches for user details and images
    private final Map<Long, UserDto> userCache = new HashMap<>();
    private final Map<Long, List<UserImageEntity>> imageCache = new HashMap<>();

    @Override
    public List<ShowProfileDto> getConnections(long userId) {
        log.debug("Fetching connections for userId: {}", userId);

        List<ConnectionsEntity> connections = connectionDao.getConnection(userId);

        if (connections == null || connections.isEmpty()) {
            log.info("No connections found for userId: {}", userId);
            return Collections.emptyList();
        }

        List<Long> connectedUserIds = connections.stream()
                .map(connection -> connection.getUserId1() == userId ? connection.getUserId2() : connection.getUserId1())
                .toList();
        Set<Long> blockedIds = safetyService.blockedUserIds(userId, connectedUserIds);
        List<ShowProfileDto> profiles = new ArrayList<>();

        for (ConnectionsEntity connection : connections) {
            long connectedUserId = (connection.getUserId1() == userId)
                    ? connection.getUserId2()
                    : connection.getUserId1();

            if (blockedIds.contains(connectedUserId)) {
                continue;
            }

            try {
                UserDto userDto = getUserDetailsCached(connectedUserId);

                // Filtering logic
                if (!connection.isActive()) {
                    log.info("Skipping userId: {} due to blocked or deactivated status", connectedUserId);
                    continue;
                }

                List<UserImageEntity> images = getUserImagesCached(connectedUserId);

                ShowProfileDto profile = new ShowProfileDto();
                profile.setId(UUID.randomUUID().toString());
                profile.setUser(userDto);
                profile.setUserImages(images);

                profiles.add(profile);

            } catch (DataNotFoundException ex) {
                log.warn("Skipping userId: {} due to missing data: {}", connectedUserId, ex.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error while processing userId: {}", connectedUserId, e);
            }
        }

        log.info("Returning {} connected profiles for userId: {}", profiles.size(), userId);
        return profiles;
    }

    private UserDto getUserDetailsCached(long userId) {
        if (userCache.containsKey(userId)) {
            return userCache.get(userId);
        }

        try {
            log.debug("Fetching user details for userId: {}", userId);

            ResponseEntity<SuccessResponseHandler<UserDto>> response = userFeingClient.getUser(userId);
            UserDto user = Optional.ofNullable(response.getBody())
                    .map(SuccessResponseHandler::getBody)
                    .orElse(null);

            if (user == null) {
                throw new DataNotFoundException("User not found for ID: " + userId);
            }

            userCache.put(userId, user);
            return user;

        } catch (Exception e) {
            log.error("Failed to fetch user details for userId: {}", userId, e);
            throw new DataNotFoundException("User not found or external service failed");
        }
    }

    private List<UserImageEntity> getUserImagesCached(long userId) {
        if (imageCache.containsKey(userId)) {
            return imageCache.get(userId);
        }

        try {
            log.debug("Fetching user images for userId: {}", userId);

            ResponseEntity<SuccessResponseHandler<List<UserImageEntity>>> response = userFeingClient.getUserImages(userId);
            List<UserImageEntity> images = Optional.ofNullable(response.getBody())
                    .map(SuccessResponseHandler::getBody)
                    .orElse(null);

            if (images == null) {
                throw new DataNotFoundException("User images not found for ID: " + userId);
            }

            imageCache.put(userId, images);
            return images;

        } catch (Exception e) {
            log.error("Failed to fetch user images for userId: {}", userId, e);
            throw new DataNotFoundException("User images not found or external service failed");
        }
    }
}
