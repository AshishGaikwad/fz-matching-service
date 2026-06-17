package tech.grastone.fz.matching.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dao.ConnectionDao;
import tech.grastone.fz.matching.dto.LowkeyCompatibilityResultDto;
import tech.grastone.fz.matching.dto.LowkeyDiscoverDto;
import tech.grastone.fz.matching.dto.LowkeyEnterRequestDto;
import tech.grastone.fz.matching.dto.LowkeyLeaveRequestDto;
import tech.grastone.fz.matching.dto.LowkeyLocationUpdateRequestDto;
import tech.grastone.fz.matching.dto.LowkeyRequestDto;
import tech.grastone.fz.matching.dto.LowkeySessionDto;
import tech.grastone.fz.matching.dto.NotificationDto;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.SendMatchRequestDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.LowkeyDiscoveryHistoryEntity;
import tech.grastone.fz.matching.entity.LowkeySessionEntity;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.entity.MatchScoreCacheEntity;
import tech.grastone.fz.matching.entity.UserImageEntity;
import tech.grastone.fz.matching.entity.UserLocationEntity;
import tech.grastone.fz.matching.enums.LowkeySessionStatus;
import tech.grastone.fz.matching.enums.LookingFor;
import tech.grastone.fz.matching.enums.RequestStatus;
import tech.grastone.fz.matching.enums.SubscriptionPlan;
import tech.grastone.fz.matching.exception.DataNotFoundException;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.repository.LowkeyDiscoveryHistoryRepository;
import tech.grastone.fz.matching.repository.LowkeySessionRepository;
import tech.grastone.fz.matching.repository.MatchRequestRepository;
import tech.grastone.fz.matching.repository.MatchScoreCacheRepository;
import tech.grastone.fz.matching.repository.UserLocationRepository;
import tech.grastone.fz.matching.service.LowkeyService;
import tech.grastone.fz.matching.service.MatchingService;
import tech.grastone.fz.matching.service.PreferencesService;
import tech.grastone.fz.matching.service.SafetyService;
import tech.grastone.fz.matching.service.client.MessagingFeingClient;
import tech.grastone.fz.matching.service.client.UserFeingClient;

@Service
@AllArgsConstructor
@Slf4j
public class LowkeyServiceImpl implements LowkeyService {

    private static final int DEFAULT_RADIUS_KM = 25;
    private static final int MAX_RADIUS_KM = 50;
    private static final int DEFAULT_DURATION_MINUTES = 60;
    private static final int FREE_MAX_DURATION_MINUTES = 60;
    private static final int PREMIUM_MAX_DURATION_MINUTES = 24 * 60;
    private static final int SCORE_CACHE_TTL_MINUTES = 5;

    private final LowkeySessionRepository lowkeySessionRepository;
    private final UserLocationRepository userLocationRepository;
    private final LowkeyDiscoveryHistoryRepository discoveryHistoryRepository;
    private final MatchScoreCacheRepository matchScoreCacheRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final UserFeingClient userFeingClient;
    private final MessagingFeingClient messagingFeingClient;
    private final PreferencesService preferencesService;
    private final MatchingService matchingService;
    private final ConnectionDao connectionDao;
    private final SafetyService safetyService;
    private final LowkeyCompatibilityEngine compatibilityEngine;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public LowkeySessionDto getMySession(Long userId) {
        expireOldSessions();
        return lowkeySessionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, LowkeySessionStatus.ACTIVE)
                .map(this::toSessionDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public LowkeySessionDto enter(Long userId, LowkeyEnterRequestDto request) {
        expireOldSessions();

        UserDto user = getUserDetails(userId);
        double latitude = firstNonNull(request == null ? null : request.getLatitude(), user.getLattitude());
        double longitude = firstNonNull(request == null ? null : request.getLongitude(), user.getLongitude());
        validateCoordinates(latitude, longitude);

        LocalDateTime now = LocalDateTime.now();
        int radiusKm = normalizeRadius(request == null ? null : request.getRadiusKm());
        int durationMinutes = normalizeDuration(user, request == null ? null : request.getDurationMinutes());
        Set<LookingFor> lookingFor = normalizeLookingFor(
                request == null ? null : request.getLookingFor(),
                user.getLookingFor()
        );

        leaveActiveSessions(userId, null, now);

        LowkeySessionEntity session = new LowkeySessionEntity();
        session.setUserId(userId);
        session.setStatus(LowkeySessionStatus.ACTIVE);
        session.setLatitude(latitude);
        session.setLongitude(longitude);
        session.setLocationAccuracyMeters(request == null ? null : request.getLocationAccuracyMeters());
        session.setRadiusKm(radiusKm);
        session.setDurationMinutes(durationMinutes);
        session.setLookingForValues(serializeLookingFor(lookingFor));
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        session.setExpiresAt(now.plusYears(100));
        session.setCreatedBy(String.valueOf(userId));

        LowkeySessionEntity saved = lowkeySessionRepository.save(session);
        upsertUserLocation(userId, latitude, longitude, saved.getLocationAccuracyMeters(), now);
        publishLowkeyEvent("USER_ENTERED_LOWKEY", saved, userId, null, Map.of(
                "sessionId", saved.getSessionId(),
                "userName", safeName(user),
                "radiusKm", radiusKm
        ));

        return toSessionDto(saved);
    }

    @Override
    @Transactional
    public LowkeySessionDto updateLocation(Long userId, LowkeyLocationUpdateRequestDto request) {
        if (request == null) {
            throw new ValidationException("Location update is required");
        }

        LowkeySessionEntity session = resolveActiveSession(userId, request.getSessionId());
        double latitude = firstNonNull(request.getLatitude(), session.getLatitude());
        double longitude = firstNonNull(request.getLongitude(), session.getLongitude());
        validateCoordinates(latitude, longitude);

        LocalDateTime now = LocalDateTime.now();
        session.setLatitude(latitude);
        session.setLongitude(longitude);
        session.setLocationAccuracyMeters(request.getLocationAccuracyMeters());
        session.setLastSeenAt(now);
        session.setUpdatedBy(String.valueOf(userId));

        LowkeySessionEntity saved = lowkeySessionRepository.save(session);
        upsertUserLocation(userId, latitude, longitude, request.getLocationAccuracyMeters(), now);
        publishLowkeyEvent("LOCATION_UPDATED", saved, userId, null, Map.of(
                "sessionId", saved.getSessionId(),
                "latitude", latitude,
                "longitude", longitude
        ));

        return toSessionDto(saved);
    }

    @Override
    @Transactional
    public LowkeySessionDto leave(Long userId, LowkeyLeaveRequestDto request) {
        expireOldSessions();
        LocalDateTime now = LocalDateTime.now();
        List<LowkeySessionEntity> sessions = leaveActiveSessions(
                userId,
                request == null ? null : request.getSessionId(),
                now
        );

        if (sessions.isEmpty()) {
            throw new DataNotFoundException("Active Lowkey session not found");
        }

        LowkeySessionEntity left = sessions.get(0);
        publishLowkeyEvent("USER_LEFT_LOWKEY", left, userId, null, Map.of(
                "sessionId", left.getSessionId(),
                "userId", userId
        ));

        return toSessionDto(left);
    }

    @Override
    @Transactional
    public List<LowkeyDiscoverDto> discover(Long userId, Pageable pageable) {
        expireOldSessions();
        LocalDateTime now = LocalDateTime.now();
        LowkeySessionEntity me = resolveActiveSession(userId, null);

        UserDto currentUser = getUserDetails(userId);
        PreferencesDto currentPreference = safeGetPreference(userId);
        BoundingBox box = boundingBox(me.getLatitude(), me.getLongitude(), me.getRadiusKm());
        Pageable candidatePage = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(150, Math.max(pageable.getPageSize() * 4, pageable.getPageSize()))
        );

        List<LowkeySessionEntity> candidates = lowkeySessionRepository.findNearbyCandidates(
                userId,
                box.minLat(),
                box.maxLat(),
                box.minLon(),
                box.maxLon(),
                candidatePage
        );
        Set<Long> blockedIds = safetyService.blockedUserIds(userId,
                candidates.stream().map(LowkeySessionEntity::getUserId).toList());
        Map<Long, LowkeyDiscoveryHistoryEntity> historyByCandidate = loadHistory(
                userId,
                candidates.stream().map(LowkeySessionEntity::getUserId).toList()
        );

        List<LowkeyDiscoverDto> results = new ArrayList<>();
        for (LowkeySessionEntity candidateSession : candidates) {
            if (blockedIds.contains(candidateSession.getUserId())) {
                continue;
            }
            double distanceKm = distanceKm(
                    me.getLatitude(),
                    me.getLongitude(),
                    candidateSession.getLatitude(),
                    candidateSession.getLongitude()
            );
            if (distanceKm > me.getRadiusKm() || distanceKm > Math.max(1, candidateSession.getRadiusKm())) {
                continue;
            }
            if (hasExistingConnection(userId, candidateSession.getUserId())) {
                continue;
            }
            if (hasPendingRequest(userId, candidateSession.getUserId())) {
                continue;
            }

            UserDto candidate = getUserDetails(candidateSession.getUserId());
            PreferencesDto candidatePreference = safeGetPreference(candidateSession.getUserId());
            LowkeyDiscoveryHistoryEntity history = historyByCandidate.get(candidateSession.getUserId());
            LowkeyCompatibilityResultDto compatibility = compatibilityEngine.score(
                    currentUser,
                    currentPreference,
                    me,
                    candidate,
                    candidatePreference,
                    candidateSession,
                    history,
                    distanceKm
            );

            upsertScoreCache(userId, candidate.getId(), compatibility, now);

            LowkeyDiscoverDto dto = new LowkeyDiscoverDto();
            dto.setId(UUID.randomUUID().toString());
            dto.setSessionId(candidateSession.getSessionId());
            dto.setUser(candidate);
            dto.setPreference(candidatePreference);
            dto.setUserImages(getUserImages(candidate.getId()));
            dto.setDistanceKm(distanceKm);
            dto.setCompatibilityScore(compatibility.getScore());
            dto.setCompatibilityPercentage(compatibility.getScore());
            dto.setMatchGrade(compatibility.getMatchGrade());
            dto.setMatchExplanation(compatibility.getMatchExplanation());
            dto.setScoreBreakdown(compatibility.getBreakdown());
            dto.setFreshnessScore(compatibility.getFreshnessScore());
            dto.setBadges(buildBadges(candidate, compatibility, distanceKm));
            dto.setOnlineStatus(onlineStatus(candidateSession.getLastSeenAt()));
            dto.setRequestStatus(resolveRequestStatus(userId, candidate.getId()));
            results.add(dto);
        }

        List<LowkeyDiscoverDto> ranked = results.stream()
                .sorted(Comparator
                        .comparingInt(LowkeyDiscoverDto::getCompatibilityScore).reversed()
                        .thenComparingInt(LowkeyDiscoverDto::getFreshnessScore).reversed()
                        .thenComparingDouble(LowkeyDiscoverDto::getDistanceKm))
                .limit(pageable.getPageSize())
                .toList();

        recordDiscoveryHistory(userId, ranked, now);
        if (!ranked.isEmpty()) {
            publishLowkeyEvent("MATCH_FOUND", me, userId, null, Map.of(
                    "sessionId", me.getSessionId(),
                    "count", ranked.size()
            ));
        }

        return ranked;
    }

    @Override
    @Transactional
    public MatchRequestEntity sendRequest(Long userId, LowkeyRequestDto request) {
        if (request == null || request.getReceiverId() == null) {
            throw new ValidationException("Receiver ID is required");
        }
        resolveActiveSession(userId, request.getSessionId());
        safetyService.assertNotBlocked(userId, request.getReceiverId());

        SendMatchRequestDto dto = new SendMatchRequestDto();
        dto.setSenderId(userId);
        dto.setReceiverId(request.getReceiverId());
        dto.setRequestMessage(limit(request.getRequestMessage(), 100));
        MatchRequestEntity saved = matchingService.sendRequest(dto);

        UserDto sender = getUserDetails(userId);
        sendNotificationSafe(
                request.getReceiverId(),
                "You have a new connection request",
                safeName(sender) + " sent you a request."
        );
        publishLowkeyEvent("REQUEST_SENT", null, userId, request.getReceiverId(), Map.of(
                "requestId", saved.getMatchRequestId(),
                "receiverId", request.getReceiverId()
        ));
        return saved;
    }

    private LowkeySessionEntity resolveActiveSession(Long userId, Long sessionId) {
        if (sessionId != null) {
            LowkeySessionEntity session = lowkeySessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ValidationException("Lowkey session not found"));
            if (!session.getUserId().equals(userId)
                    || session.getStatus() != LowkeySessionStatus.ACTIVE) {
                throw new ValidationException("Enter Lowkey before discovering people");
            }
            return session;
        }

        return lowkeySessionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, LowkeySessionStatus.ACTIVE)
                .orElseThrow(() -> new ValidationException("Enter Lowkey before discovering people"));
    }

    private List<LowkeySessionEntity> leaveActiveSessions(Long userId, Long sessionId, LocalDateTime now) {
        List<LowkeySessionEntity> active = lowkeySessionRepository
                .findByUserIdAndStatus(userId, LowkeySessionStatus.ACTIVE).stream()
                .filter(session -> sessionId == null || session.getSessionId().equals(sessionId))
                .toList();

        active.forEach(session -> {
            session.setStatus(LowkeySessionStatus.LEFT);
            session.setLeftAt(now);
            session.setUpdatedBy(String.valueOf(userId));
        });
        lowkeySessionRepository.saveAll(active);
        return active;
    }

    private LowkeySessionDto toSessionDto(LowkeySessionEntity session) {
        LowkeySessionDto dto = new LowkeySessionDto();
        dto.setSessionId(session.getSessionId());
        dto.setActive(session.getStatus() == LowkeySessionStatus.ACTIVE);
        dto.setRadiusKm(session.getRadiusKm());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setLatitude(session.getLatitude());
        dto.setLongitude(session.getLongitude());
        dto.setLocationAccuracyMeters(session.getLocationAccuracyMeters());
        dto.setEnteredAt(session.getEnteredAt());
        dto.setExpiresAt(session.getExpiresAt());
        dto.setRemainingSeconds(0);
        dto.setParticipantCount(lowkeySessionRepository.countByStatus(
                LowkeySessionStatus.ACTIVE
        ));
        dto.setLookingFor(parseLookingFor(session.getLookingForValues()));
        return dto;
    }

    private void upsertUserLocation(Long userId, double latitude, double longitude, Integer accuracyMeters,
            LocalDateTime now) {
        UserLocationEntity location = userLocationRepository.findById(userId).orElseGet(UserLocationEntity::new);
        location.setUserId(userId);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracyMeters(accuracyMeters);
        location.setSource("LOWKEY");
        location.setLastUpdatedAt(now);
        location.setUpdatedBy(String.valueOf(userId));
        if (location.getCreatedBy() == null) {
            location.setCreatedBy(String.valueOf(userId));
        }
        userLocationRepository.save(location);
    }

    private Map<Long, LowkeyDiscoveryHistoryEntity> loadHistory(Long viewerUserId, Collection<Long> candidateUserIds) {
        if (candidateUserIds == null || candidateUserIds.isEmpty()) {
            return Map.of();
        }
        return discoveryHistoryRepository
                .findByIdViewerUserIdAndIdCandidateUserIdIn(viewerUserId, candidateUserIds).stream()
                .collect(Collectors.toMap(
                        LowkeyDiscoveryHistoryEntity::getCandidateUserId,
                        Function.identity()
                ));
    }

    private void recordDiscoveryHistory(Long viewerUserId, List<LowkeyDiscoverDto> results, LocalDateTime now) {
        List<LowkeyDiscoveryHistoryEntity> updated = new ArrayList<>();
        for (LowkeyDiscoverDto result : results) {
            Long candidateId = result.getUser().getId();
            LowkeyDiscoveryHistoryEntity history = discoveryHistoryRepository
                    .findByIdViewerUserIdAndIdCandidateUserId(viewerUserId, candidateId)
                    .orElseGet(LowkeyDiscoveryHistoryEntity::new);
            history.setViewerUserId(viewerUserId);
            history.setCandidateUserId(candidateId);
            history.setExposureCount((history.getExposureCount() == null ? 0 : history.getExposureCount()) + 1);
            history.setLastScore(result.getCompatibilityScore());
            history.setLastSeenAt(now);
            history.setUpdatedBy(String.valueOf(viewerUserId));
            if (history.getCreatedBy() == null) {
                history.setCreatedBy(String.valueOf(viewerUserId));
            }
            updated.add(history);
        }
        discoveryHistoryRepository.saveAll(updated);
    }

    private void upsertScoreCache(Long viewerUserId, Long candidateUserId, LowkeyCompatibilityResultDto compatibility,
            LocalDateTime now) {
        MatchScoreCacheEntity cache = matchScoreCacheRepository
                .findByIdViewerUserIdAndIdCandidateUserId(viewerUserId, candidateUserId)
                .orElseGet(MatchScoreCacheEntity::new);
        cache.setViewerUserId(viewerUserId);
        cache.setCandidateUserId(candidateUserId);
        cache.setScore(compatibility.getScore());
        cache.setMatchGrade(compatibility.getMatchGrade());
        cache.setMatchExplanation(compatibility.getMatchExplanation());
        cache.setScoreBreakdown(toJson(compatibility.getBreakdown()));
        cache.setExpiresAt(now.plusMinutes(SCORE_CACHE_TTL_MINUTES));
        cache.setUpdatedBy(String.valueOf(viewerUserId));
        if (cache.getCreatedBy() == null) {
            cache.setCreatedBy(String.valueOf(viewerUserId));
        }
        matchScoreCacheRepository.save(cache);
    }

    private List<String> buildBadges(UserDto candidate, LowkeyCompatibilityResultDto compatibility, double distanceKm) {
        List<String> badges = new ArrayList<>();
        badges.add(compatibility.getMatchGrade() + " Match");
        if (distanceKm <= 2) {
            badges.add("Nearby");
        }
        if (candidate.getProfession() != null && !candidate.getProfession().isBlank()) {
            badges.add(candidate.getProfession());
        }
        if (candidate.getLookingFor() != null && !candidate.getLookingFor().isEmpty()) {
            badges.add(label(candidate.getLookingFor().iterator().next()));
        }
        return badges.stream().limit(4).toList();
    }

    private String resolveRequestStatus(Long senderId, Long receiverId) {
        Optional<MatchRequestEntity> request = matchRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId)
                .stream()
                .findFirst();
        if (request.isPresent()) {
            return request.get().getRequestStatus().name();
        }

        return matchRequestRepository.findBySenderIdAndReceiverId(receiverId, senderId).stream()
                .findFirst()
                .map(entity -> entity.getRequestStatus() == RequestStatus.PENDING
                        ? "RECEIVED"
                        : entity.getRequestStatus().name())
                .orElse(null);
    }

    private void expireOldSessions() {
        // Lowkey is persistent until the user explicitly leaves.
        // We keep this hook so older callers remain safe, but it no longer auto-expires sessions.
    }

    private UserDto getUserDetails(long userId) {
        try {
            ResponseEntity<SuccessResponseHandler<UserDto>> response = userFeingClient.getUser(userId);
            UserDto user = Optional.ofNullable(response.getBody()).map(SuccessResponseHandler::getBody).orElse(null);
            if (user == null) {
                throw new DataNotFoundException("User not found for ID: " + userId);
            }
            return user;
        } catch (Exception e) {
            log.error("Failed to fetch user details for userId: {}", userId, e);
            throw new DataNotFoundException("User not found or external service failed");
        }
    }

    private PreferencesDto safeGetPreference(Long userId) {
        try {
            return preferencesService.get(userId.intValue());
        } catch (Exception ignored) {
            PreferencesDto fallback = new PreferencesDto();
            fallback.setUserId(userId);
            fallback.setMinAge(18);
            fallback.setMaxAge(60);
            fallback.setDistance(DEFAULT_RADIUS_KM);
            return fallback;
        }
    }

    private List<UserImageEntity> getUserImages(long userId) {
        try {
            ResponseEntity<SuccessResponseHandler<List<UserImageEntity>>> response = userFeingClient.getUserImages(userId);
            return Optional.ofNullable(response.getBody()).map(SuccessResponseHandler::getBody).orElse(List.of());
        } catch (Exception e) {
            log.warn("Failed to fetch user images for userId: {}", userId);
            return List.of();
        }
    }

    private boolean hasExistingConnection(Long userId, Long candidateId) {
        Long userId1 = Math.min(userId, candidateId);
        Long userId2 = Math.max(userId, candidateId);
        return !connectionDao.getConnectionByUserId1AndUserId2(userId1, userId2).isEmpty();
    }

    private boolean hasPendingRequest(Long senderId, Long receiverId) {
        return matchRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId).stream()
                .anyMatch(request -> request.getRequestStatus() == RequestStatus.PENDING)
                || matchRequestRepository.findBySenderIdAndReceiverId(receiverId, senderId).stream()
                .anyMatch(request -> request.getRequestStatus() == RequestStatus.PENDING);
    }

    private void sendNotificationSafe(Long toUserId, String title, String message) {
        try {
            messagingFeingClient.sendNotification(NotificationDto.builder()
                    .toUserId(toUserId)
                    .notificationTitle(title)
                    .notificationMessage(message)
                    .build());
        } catch (Exception e) {
            log.warn("Unable to send Lowkey notification to {}", toUserId);
        }
    }

    private void publishLowkeyEvent(String type, LowkeySessionEntity session, Long userId, Long targetUserId,
            Map<String, Object> payload) {
        log.info("Lowkey event recorded locally only: type={}, userId={}, targetUserId={}, payloadKeys={}",
                type,
                userId,
                targetUserId,
                payload == null ? List.of() : payload.keySet());
    }

    private int normalizeRadius(Integer requestedRadius) {
        int radius = requestedRadius == null ? DEFAULT_RADIUS_KM : requestedRadius;
        return Math.min(MAX_RADIUS_KM, Math.max(1, radius));
    }

    private int normalizeDuration(UserDto user, Integer requestedDuration) {
        int duration = requestedDuration == null ? DEFAULT_DURATION_MINUTES : requestedDuration;
        int max = isFreeUser(user) ? FREE_MAX_DURATION_MINUTES : PREMIUM_MAX_DURATION_MINUTES;
        return Math.min(max, Math.max(15, duration));
    }

    private boolean isFreeUser(UserDto user) {
        return user.getSubscriptionPlan() == null
                || user.getSubscriptionPlan() == SubscriptionPlan.FREE
                || user.getPlanExpiryDate() == null
                || user.getPlanExpiryDate().isBefore(LocalDate.now());
    }

    private Set<LookingFor> normalizeLookingFor(Set<LookingFor> requested, Set<LookingFor> fallback) {
        if (requested != null && !requested.isEmpty()) {
            return requested;
        }
        return fallback == null ? Set.of() : fallback;
    }

    private String serializeLookingFor(Set<LookingFor> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private Set<LookingFor> parseLookingFor(String values) {
        if (values == null || values.isBlank()) {
            return Set.of();
        }
        Set<LookingFor> parsed = new LinkedHashSet<>();
        for (String value : values.split(",")) {
            try {
                parsed.add(LookingFor.valueOf(value.trim()));
            } catch (IllegalArgumentException ignored) {
                // Skip stale values from older mobile builds.
            }
        }
        return parsed;
    }

    private String label(LookingFor value) {
        String text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private String onlineStatus(LocalDateTime lastSeenAt) {
        return "AVAILABLE";
    }

    private String safeName(UserDto user) {
        return user.getFullName() == null ? "Frenzo user" : user.getFullName();
    }

    private String toJson(Map<String, Integer> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private double firstNonNull(Double requested, double fallback) {
        return requested == null ? fallback : requested;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("Valid location is required for Lowkey");
        }
    }

    private long secondsUntil(LocalDateTime endsAt) {
        return Math.max(0, Duration.between(LocalDateTime.now(), endsAt).getSeconds());
    }

    private BoundingBox boundingBox(double latitude, double longitude, int radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / Math.max(1.0, 111.0 * Math.cos(Math.toRadians(latitude)));
        return new BoundingBox(
                Math.max(-90, latitude - latDelta),
                Math.min(90, latitude + latDelta),
                Math.max(-180, longitude - lonDelta),
                Math.min(180, longitude + lonDelta)
        );
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }

    private record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
    }
}
