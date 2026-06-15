package tech.grastone.fz.matching.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dao.ConnectionDao;
import tech.grastone.fz.matching.dao.VibeDiscoveryDao;
import tech.grastone.fz.matching.dto.ActiveVibeDto;
import tech.grastone.fz.matching.dto.JoinVibeRequestDto;
import tech.grastone.fz.matching.dto.LeaveVibeRequestDto;
import tech.grastone.fz.matching.dto.NotificationDto;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.dto.VibeCandidateRowDto;
import tech.grastone.fz.matching.dto.VibeDiscoverDto;
import tech.grastone.fz.matching.dto.VibeDto;
import tech.grastone.fz.matching.dto.VibeRequestDto;
import tech.grastone.fz.matching.dto.VibeRequestReplyDto;
import tech.grastone.fz.matching.dto.VibeSocketEventDto;
import tech.grastone.fz.matching.entity.ActiveVibeSessionEntity;
import tech.grastone.fz.matching.entity.ConnectionsEntity;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.entity.UserImageEntity;
import tech.grastone.fz.matching.entity.UserVibeParticipationEntity;
import tech.grastone.fz.matching.entity.VibeConnectionEntity;
import tech.grastone.fz.matching.entity.VibeEntity;
import tech.grastone.fz.matching.enums.ConnectionStatus;
import tech.grastone.fz.matching.enums.Drinking;
import tech.grastone.fz.matching.enums.Gender;
import tech.grastone.fz.matching.enums.Lifestyle;
import tech.grastone.fz.matching.enums.Orientation;
import tech.grastone.fz.matching.enums.Personality;
import tech.grastone.fz.matching.enums.Religion;
import tech.grastone.fz.matching.enums.RequestStatus;
import tech.grastone.fz.matching.enums.Smoking;
import tech.grastone.fz.matching.enums.SubscriptionPlan;
import tech.grastone.fz.matching.enums.VibeActivityType;
import tech.grastone.fz.matching.enums.VibeConnectionStatus;
import tech.grastone.fz.matching.enums.VibeParticipationStatus;
import tech.grastone.fz.matching.enums.VibeSessionStatus;
import tech.grastone.fz.matching.exception.DataNotFoundException;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.repository.ActiveVibeSessionRepository;
import tech.grastone.fz.matching.repository.MatchRequestRepository;
import tech.grastone.fz.matching.repository.UserVibeParticipationRepository;
import tech.grastone.fz.matching.repository.VibeConnectionRepository;
import tech.grastone.fz.matching.repository.VibeRepository;
import tech.grastone.fz.matching.service.PreferencesService;
import tech.grastone.fz.matching.service.VibeService;
import tech.grastone.fz.matching.service.client.MessagingFeingClient;
import tech.grastone.fz.matching.service.client.UserFeingClient;

@Service
@AllArgsConstructor
@Slf4j
public class VibeServiceImpl implements VibeService {

    private static final int FREE_MIN_RADIUS_KM = 0;
    private static final int MAX_RADIUS_KM = 50;
    private static final int FREE_MAX_DURATION_MINUTES = 60;
    private static final int PREMIUM_MAX_DURATION_MINUTES = 24 * 60;

    private final VibeRepository vibeRepository;
    private final ActiveVibeSessionRepository sessionRepository;
    private final UserVibeParticipationRepository participationRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final VibeConnectionRepository vibeConnectionRepository;
    private final VibeDiscoveryDao vibeDiscoveryDao;
    private final UserFeingClient userFeingClient;
    private final MessagingFeingClient messagingFeingClient;
    private final PreferencesService preferencesService;
    private final ConnectionDao connectionDao;

    @Override
    @Transactional
    public List<VibeDto> getVibes() {
        ensureDefaultVibes();
        expireOldParticipations();
        return vibeRepository.findByActiveTrueOrderBySortOrderAsc().stream().map(this::toVibeDto).toList();
    }

    @Override
    @Transactional
    public List<VibeDto> getNearbyVibes(Long userId, Double latitude, Double longitude, Integer radiusKm) {
        ensureDefaultVibes();
        expireOldParticipations();
        UserDto user = getUserDetails(userId);
        int radius = normalizeRadius(user, radiusKm);

        return vibeRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toVibeDto)
                .sorted(Comparator.comparingLong(VibeDto::getParticipantCount).reversed())
                .peek(vibe -> vibe.setDescription(vibe.getDescription() + " nearby within " + radius + " km"))
                .toList();
    }

    @Override
    @Transactional
    public ActiveVibeDto getMyActiveVibe(Long userId) {
        expireOldParticipations();
        return participationRepository.findFirstByUserIdAndStatusAndExpiresAtAfterOrderByJoinedAtDesc(
                userId, VibeParticipationStatus.ACTIVE, LocalDateTime.now()).map(this::toActiveVibeDto).orElse(null);
    }

    @Override
    @Transactional
    public ActiveVibeDto joinVibe(Long userId, JoinVibeRequestDto request) {
        if (request == null || request.getVibeId() == null) {
            throw new ValidationException("Vibe ID is required");
        }
        ensureDefaultVibes();
        expireOldParticipations();

        UserDto user = getUserDetails(userId);
        VibeEntity vibe = vibeRepository.findByVibeIdAndActiveTrue(request.getVibeId())
                .orElseThrow(() -> new DataNotFoundException("Vibe not found"));
        int radiusKm = normalizeRadius(user, request.getRadiusKm());
        int durationMinutes = normalizeDuration(user, request.getDurationMinutes(), vibe.getDefaultDurationMinutes());
        double latitude = firstNonNull(request.getLatitude(), user.getLattitude());
        double longitude = firstNonNull(request.getLongitude(), user.getLongitude());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requestedEndsAt = now.plusMinutes(durationMinutes);

        leaveCurrentParticipations(userId, null, now);
        ActiveVibeSessionEntity session = sessionRepository
                .findFirstByVibeIdAndStatusAndEndsAtAfterOrderByEndsAtAsc(vibe.getVibeId(), VibeSessionStatus.ACTIVE, now)
                .orElseGet(() -> createSession(vibe, now, requestedEndsAt, userId));

        if (session.getEndsAt().isBefore(requestedEndsAt)) {
            session.setEndsAt(requestedEndsAt);
            session.setUpdatedBy(String.valueOf(userId));
            session = sessionRepository.save(session);
        }

        UserVibeParticipationEntity participation = participationRepository.findByUserIdAndSessionId(userId, session.getSessionId())
                .orElseGet(UserVibeParticipationEntity::new);
        participation.setUserId(userId);
        participation.setSessionId(session.getSessionId());
        participation.setVibeId(vibe.getVibeId());
        participation.setLatitude(latitude);
        participation.setLongitude(longitude);
        participation.setRadiusKm(radiusKm);
        participation.setStatus(VibeParticipationStatus.ACTIVE);
        participation.setJoinedAt(now);
        participation.setLeftAt(null);
        participation.setExpiresAt(session.getEndsAt());
        participation.setCreatedBy(String.valueOf(userId));
        participation.setUpdatedBy(String.valueOf(userId));
        participationRepository.save(participation);

        ActiveVibeDto activeVibe = toActiveVibeDto(participation);
        publishVibeEvent("VIBE_JOINED", activeVibe, userId, null, Map.of("vibeName", vibe.getName(), "userName", user.getFullName()));
        publishVibeEvent("USER_DISCOVERED", activeVibe, userId, null, Map.of("userId", userId));
        return activeVibe;
    }

    @Override
    @Transactional
    public ActiveVibeDto leaveVibe(Long userId, LeaveVibeRequestDto request) {
        expireOldParticipations();
        LocalDateTime now = LocalDateTime.now();
        List<UserVibeParticipationEntity> participations = leaveCurrentParticipations(userId, request == null ? null : request.getSessionId(), now);
        if (participations.isEmpty()) {
            throw new DataNotFoundException("Active vibe not found");
        }
        ActiveVibeDto activeVibe = toActiveVibeDto(participations.get(0));
        publishVibeEvent("VIBE_LEFT", activeVibe, userId, null, Map.of("userId", userId));
        return activeVibe;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VibeDiscoverDto> discover(Long userId, Long vibeId, Long sessionId, Pageable pageable) {
        UserVibeParticipationEntity me = resolveActiveParticipation(userId, vibeId, sessionId);
        UserDto currentUser = getUserDetails(userId);
        PreferencesDto currentPreference = safeGetPreference(userId);
        VibeDto vibe = toVibeDto(getVibe(me.getVibeId()));

        List<VibeDiscoverDto> results = new ArrayList<>();
        List<VibeCandidateRowDto> rows = vibeDiscoveryDao.discoverCandidates(currentUser, me.getSessionId(),
                me.getLatitude(), me.getLongitude(), me.getRadiusKm(), pageable);

        for (VibeCandidateRowDto row : rows) {
            UserDto candidate = row.getUser();
            if (!isVisibleGenderForVibe(currentUser, candidate)) {
                continue;
            }
            if (hasPendingMatchRequest(userId, candidate.getId())) {
                continue;
            }
            int score = calculateCompatibility(currentUser, currentPreference, row, getVibe(me.getVibeId()));
            VibeDiscoverDto dto = new VibeDiscoverDto();
            dto.setId(UUID.randomUUID().toString());
            dto.setSessionId(row.getSessionId());
            dto.setVibe(vibe);
            dto.setUser(candidate);
            dto.setDistanceKm(row.getDistanceKm());
            dto.setCompatibilityPercentage(score);
            dto.setBadges(buildBadges(row.getPreference(), getVibe(me.getVibeId()), row.getDistanceKm()));
            dto.setUserImages(getUserImages(candidate.getId()));
            dto.setRequestStatus(resolveRequestStatus(userId, candidate.getId()));
            results.add(dto);
        }
        return results;
    }

    @Override
    @Transactional
    public MatchRequestEntity sendRequest(Long userId, VibeRequestDto request) {
        if (request == null || request.getReceiverId() == null) {
            throw new ValidationException("Receiver ID is required");
        }
        if (userId.equals(request.getReceiverId())) {
            throw new ValidationException("You cannot send a vibe request to yourself");
        }
        UserVibeParticipationEntity me = resolveActiveParticipation(userId, request.getVibeId(), request.getSessionId());
        participationRepository.findByUserIdAndSessionIdAndStatusAndExpiresAtAfter(request.getReceiverId(), me.getSessionId(),
                VibeParticipationStatus.ACTIVE, LocalDateTime.now()).orElseThrow(() -> new ValidationException("This user is no longer vibing"));
        assertNoExistingVibeConnection(userId, request.getReceiverId(), me.getVibeId());

        Optional<MatchRequestEntity> reverse = matchRequestRepository.findBySenderIdAndReceiverId(request.getReceiverId(), userId)
                .stream().filter(req -> req.getRequestStatus() == RequestStatus.PENDING).findFirst();
        if (reverse.isPresent()) {
            VibeRequestReplyDto reply = new VibeRequestReplyDto();
            reply.setRequestId(reverse.get().getMatchRequestId());
            reply.setUserId(userId);
            reply.setResponseMessage("Auto-accepted because you both started vibing.");
            return acceptRequest(userId, reply);
        }

        boolean pendingExists = matchRequestRepository.findBySenderIdAndReceiverId(userId, request.getReceiverId()).stream()
                .anyMatch(req -> req.getRequestStatus() == RequestStatus.PENDING);
        if (pendingExists) {
            throw new ValidationException("Vibe request is already pending");
        }

        UserDto sender = getUserDetails(userId);
        MatchRequestEntity entity = new MatchRequestEntity();
        entity.setSenderId(userId);
        entity.setReceiverId(request.getReceiverId());
        entity.setRequestMessage(limit(request.getRequestMessage(), 100));
        entity.setRequestStatus(RequestStatus.PENDING);
        entity.setCreatedBy(String.valueOf(userId));
        entity.setUpdatedBy(String.valueOf(userId));
        MatchRequestEntity saved = matchRequestRepository.save(entity);

        sendNotificationSafe(request.getReceiverId(), "Vibe ping received",
                sender.getFullName() + " pinged you. If you ping back, we’ll lock the connection.");
        publishVibeEvent("REQUEST_SENT", toActiveVibeDto(me), userId, request.getReceiverId(), Map.of(
                "requestId", saved.getMatchRequestId(),
                "senderName", sender.getFullName(),
                "receiverId", request.getReceiverId()
        ));
        return saved;
    }

    @Override
    @Transactional
    public MatchRequestEntity acceptRequest(Long userId, VibeRequestReplyDto request) {
        MatchRequestEntity entity = getPendingMatchRequestForReply(userId, request);
        entity.setRequestStatus(RequestStatus.ACCEPT);
        entity.setReplyMessage(limit(request.getResponseMessage(), 100));
        entity.setUpdatedBy(String.valueOf(userId));
        MatchRequestEntity saved = matchRequestRepository.save(entity);
        createStandardConnectionIfMissing(saved);

        UserDto receiver = getUserDetails(userId);
        UserDto sender = getUserDetails(entity.getSenderId());
        sendNotificationSafe(entity.getSenderId(), "You’re connected", receiver.getFullName() + " matched with you.");
        sendNotificationSafe(userId, "You’re connected", sender.getFullName() + " matched with you.");
        publishVibeEvent("REQUEST_ACCEPTED", null, userId, entity.getSenderId(), Map.of("requestId", saved.getMatchRequestId()));
        return saved;
    }

    @Override
    @Transactional
    public MatchRequestEntity rejectRequest(Long userId, VibeRequestReplyDto request) {
        MatchRequestEntity entity = getPendingMatchRequestForReply(userId, request);
        entity.setRequestStatus(RequestStatus.REJECT);
        entity.setReplyMessage(limit(request.getResponseMessage(), 100));
        entity.setUpdatedBy(String.valueOf(userId));
        MatchRequestEntity saved = matchRequestRepository.save(entity);
        publishVibeEvent("REQUEST_REJECTED", null, userId, entity.getSenderId(), Map.of("requestId", saved.getMatchRequestId()));
        return saved;
    }

    private void ensureDefaultVibes() {
        if (vibeRepository.count() > 0) return;
        vibeRepository.saveAll(List.of(
                vibe("coffee", "Coffee & Chill", "Low-pressure cafe energy for easy first conversations.", "cafe", VibeActivityType.COFFEE, 1),
                vibe("walk", "Walk & Explore", "Find someone nearby for a light walk or city wander.", "walk", VibeActivityType.WALKING_EXPLORING, 2),
                vibe("gaming", "Gaming Squad", "Queue up with playful people who speak fluent GG.", "game-controller", VibeActivityType.GAMING, 3),
                vibe("fitness", "Fitness Boost", "Meet people who are active right now.", "barbell", VibeActivityType.FITNESS, 4),
                vibe("movies", "Movie Mood", "For trailers, watchlists, and last-minute plans.", "film", VibeActivityType.MOVIES, 5),
                vibe("foodie", "Foodie Run", "Discover people ready to try the next bite nearby.", "restaurant", VibeActivityType.FOODIE, 6),
                vibe("travel", "Travel Spark", "Connect around trips, places, and next escapes.", "airplane", VibeActivityType.TRAVEL, 7),
                vibe("clubbing", "Club Night", "High-energy people looking for a night-out vibe.", "musical-notes", VibeActivityType.CLUBBING, 8)
        ));
    }

    private VibeEntity vibe(String code, String name, String description, String icon, VibeActivityType type, int order) {
        VibeEntity vibe = new VibeEntity();
        vibe.setCode(code);
        vibe.setName(name);
        vibe.setDescription(description);
        vibe.setIcon(icon);
        vibe.setActivityType(type);
        vibe.setDefaultDurationMinutes(60);
        vibe.setActive(true);
        vibe.setSortOrder(order);
        vibe.setCreatedBy("system");
        return vibe;
    }

    private ActiveVibeSessionEntity createSession(VibeEntity vibe, LocalDateTime now, LocalDateTime endsAt, Long userId) {
        ActiveVibeSessionEntity session = new ActiveVibeSessionEntity();
        session.setVibeId(vibe.getVibeId());
        session.setStatus(VibeSessionStatus.ACTIVE);
        session.setStartsAt(now);
        session.setEndsAt(endsAt);
        session.setCreatedBy(String.valueOf(userId));
        return sessionRepository.save(session);
    }

    private VibeDto toVibeDto(VibeEntity vibe) {
        LocalDateTime now = LocalDateTime.now();
        VibeDto dto = new VibeDto();
        dto.setId(vibe.getVibeId());
        dto.setCode(vibe.getCode());
        dto.setName(vibe.getName());
        dto.setDescription(vibe.getDescription());
        dto.setIcon(vibe.getIcon());
        dto.setActivityType(vibe.getActivityType());
        dto.setDefaultDurationMinutes(vibe.getDefaultDurationMinutes());
        dto.setActive(vibe.isActive());
        sessionRepository.findFirstByVibeIdAndStatusAndEndsAtAfterOrderByEndsAtAsc(vibe.getVibeId(), VibeSessionStatus.ACTIVE, now)
                .ifPresent(session -> {
                    dto.setActiveSessionId(session.getSessionId());
                    dto.setEndsAt(session.getEndsAt());
                    dto.setRemainingSeconds(secondsUntil(session.getEndsAt()));
                    dto.setParticipantCount(countParticipants(session.getSessionId()));
                });
        return dto;
    }

    private ActiveVibeDto toActiveVibeDto(UserVibeParticipationEntity participation) {
        VibeEntity vibe = getVibe(participation.getVibeId());
        ActiveVibeSessionEntity session = sessionRepository.findById(participation.getSessionId())
                .orElseThrow(() -> new DataNotFoundException("Vibe session not found"));
        ActiveVibeDto dto = new ActiveVibeDto();
        dto.setSessionId(session.getSessionId());
        dto.setVibe(toVibeDto(vibe));
        dto.setJoined(participation.getStatus() == VibeParticipationStatus.ACTIVE);
        dto.setParticipantCount(countParticipants(session.getSessionId()));
        dto.setRemainingSeconds(secondsUntil(session.getEndsAt()));
        dto.setJoinedAt(participation.getJoinedAt());
        dto.setEndsAt(session.getEndsAt());
        dto.setRadiusKm(participation.getRadiusKm());
        return dto;
    }

    private UserVibeParticipationEntity resolveActiveParticipation(Long userId, Long vibeId, Long sessionId) {
        LocalDateTime now = LocalDateTime.now();
        if (sessionId != null) {
            return participationRepository.findByUserIdAndSessionIdAndStatusAndExpiresAtAfter(userId, sessionId,
                    VibeParticipationStatus.ACTIVE, now).orElseThrow(() -> new ValidationException("Join this vibe before discovering people"));
        }
        if (vibeId != null) {
            ActiveVibeSessionEntity session = sessionRepository
                    .findFirstByVibeIdAndStatusAndEndsAtAfterOrderByEndsAtAsc(vibeId, VibeSessionStatus.ACTIVE, now)
                    .orElseThrow(() -> new ValidationException("No active session for this vibe"));
            return participationRepository.findByUserIdAndSessionIdAndStatusAndExpiresAtAfter(userId, session.getSessionId(),
                    VibeParticipationStatus.ACTIVE, now).orElseThrow(() -> new ValidationException("Join this vibe before discovering people"));
        }
        return participationRepository.findFirstByUserIdAndStatusAndExpiresAtAfterOrderByJoinedAtDesc(userId,
                VibeParticipationStatus.ACTIVE, now).orElseThrow(() -> new ValidationException("Join a vibe before discovering people"));
    }

    private MatchRequestEntity getPendingMatchRequestForReply(Long userId, VibeRequestReplyDto request) {
        if (request == null || request.getRequestId() == null) throw new ValidationException("Request ID is required");
        MatchRequestEntity entity = matchRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new DataNotFoundException("Match request not found"));
        if (!entity.getReceiverId().equals(userId)) throw new ValidationException("You can only reply to requests sent to you");
        if (entity.getRequestStatus() != RequestStatus.PENDING) throw new ValidationException("This request has already been answered");
        return entity;
    }

    private int calculateCompatibility(UserDto currentUser, PreferencesDto currentPreference, VibeCandidateRowDto candidate, VibeEntity vibe) {
        int score = 20;
        score += distanceScore(candidate.getDistanceKm(), Math.min(MAX_RADIUS_KM, Math.max(1, candidate.getRadiusKm())));
        score += isMutualGenderFit(currentUser, candidate.getUser()) ? 20 : 0;
        PreferencesDto targetPreference = candidate.getPreference();
        score += same(currentPreference.getLifestyle(), targetPreference.getLifestyle()) ? 10 : 0;
        score += same(currentPreference.getPersonality(), targetPreference.getPersonality()) ? 10 : 0;
        score += same(currentPreference.getReligion(), targetPreference.getReligion()) ? 4 : 0;
        score += same(currentPreference.getDrinking(), targetPreference.getDrinking()) ? 3 : 0;
        score += same(currentPreference.getSmoking(), targetPreference.getSmoking()) ? 3 : 0;
        if (vibe.getActivityType() == VibeActivityType.COFFEE || vibe.getActivityType() == VibeActivityType.FITNESS) score += 2;
        return Math.min(100, Math.max(1, score));
    }

    private List<String> buildBadges(PreferencesDto preference, VibeEntity vibe, double distanceKm) {
        List<String> badges = new ArrayList<>();
        badges.add(label(vibe.getActivityType()));
        if (distanceKm <= 2) {
            badges.add("Nearby");
        }
        if (preference != null) {
            addIfPresent(badges, preference.getLifestyle());
            addIfPresent(badges, preference.getPersonality());
            addIfPresent(badges, preference.getDrinking());
        }
        return badges.stream().limit(4).toList();
    }

    private int distanceScore(double distanceKm, int radiusKm) {
        double normalized = Math.min(1.0, distanceKm / Math.max(1, radiusKm));
        return (int) Math.round((1.0 - normalized) * 30);
    }

    private boolean hasPendingMatchRequest(Long userId, Long candidateId) {
        return matchRequestRepository.findBySenderIdAndReceiverId(userId, candidateId).stream()
                .anyMatch(req -> req.getRequestStatus() == RequestStatus.PENDING)
                || matchRequestRepository.findBySenderIdAndReceiverId(candidateId, userId).stream()
                .anyMatch(req -> req.getRequestStatus() == RequestStatus.PENDING);
    }

    private void assertNoExistingVibeConnection(Long senderId, Long receiverId, Long vibeId) {
        Long userId1 = Math.min(senderId, receiverId);
        Long userId2 = Math.max(senderId, receiverId);
        if (vibeConnectionRepository.findByUserId1AndUserId2AndVibeId(userId1, userId2, vibeId).isPresent()) {
            throw new ValidationException("You are already connected from this vibe");
        }
    }

    private String resolveRequestStatus(Long senderId, Long receiverId) {
        return matchRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId).stream().findFirst()
                .map(req -> req.getRequestStatus().name()).orElse(null);
    }

    private List<UserVibeParticipationEntity> leaveCurrentParticipations(Long userId, Long sessionId, LocalDateTime now) {
        List<UserVibeParticipationEntity> active = participationRepository.findByUserIdAndStatus(userId, VibeParticipationStatus.ACTIVE).stream()
                .filter(participation -> sessionId == null || participation.getSessionId().equals(sessionId)).toList();
        active.forEach(participation -> {
            participation.setStatus(VibeParticipationStatus.LEFT);
            participation.setLeftAt(now);
            participation.setUpdatedBy(String.valueOf(userId));
        });
        participationRepository.saveAll(active);
        return active;
    }

    private void createStandardConnectionIfMissing(MatchRequestEntity request) {
        Long userId1 = Math.min(request.getSenderId(), request.getReceiverId());
        Long userId2 = Math.max(request.getSenderId(), request.getReceiverId());
        if (!connectionDao.getConnectionByUserId1AndUserId2(userId1, userId2).isEmpty()) return;
        ConnectionsEntity connection = new ConnectionsEntity();
        connection.setUserId1(userId1);
        connection.setUserId2(userId2);
        connection.setConnectedAt(LocalDateTime.now());
        connection.setActive(true);
        connection.setSentBy(request.getSenderId());
        connection.setAcceptedBy(request.getReceiverId());
        connection.setStatus(ConnectionStatus.ACCEPTED);
        connection.setRemarks("Created from Vibe Mode");
        connectionDao.save(connection);
    }

    private boolean isMutualGenderFit(UserDto a, UserDto b) { return isInterestedIn(a, b) && isInterestedIn(b, a); }
    private boolean isVisibleGenderForVibe(UserDto viewer, UserDto candidate) {
        if (viewer.getGender() == null || candidate.getGender() == null) return true;
        if (viewer.getGender() == Gender.MALE) return candidate.getGender() == Gender.FEMALE;
        if (viewer.getGender() == Gender.FEMALE) return candidate.getGender() == Gender.MALE;
        return true;
    }
    private boolean isInterestedIn(UserDto viewer, UserDto candidate) {
        if (viewer.getSexualOrientation() == null || viewer.getGender() == null || candidate.getGender() == null) return true;
        Orientation orientation = viewer.getSexualOrientation();
        if (orientation == Orientation.GAY) return viewer.getGender() == candidate.getGender();
        if (orientation == Orientation.LESBIAN) return candidate.getGender() == Gender.FEMALE;
        if (orientation == Orientation.STRAIGHT) return viewer.getGender() != candidate.getGender();
        return true;
    }
    private int normalizeRadius(UserDto user, Integer requestedRadius) {
        int radius = requestedRadius == null ? MAX_RADIUS_KM : requestedRadius;
        radius = Math.min(MAX_RADIUS_KM, Math.max(0, radius));
        return isFreeUser(user) ? Math.max(FREE_MIN_RADIUS_KM, radius) : radius;
    }
    private int normalizeDuration(UserDto user, Integer requestedDuration, int defaultDuration) {
        int duration = requestedDuration == null ? defaultDuration : requestedDuration;
        int max = isFreeUser(user) ? FREE_MAX_DURATION_MINUTES : PREMIUM_MAX_DURATION_MINUTES;
        return Math.min(max, Math.max(15, duration));
    }
    private boolean isFreeUser(UserDto user) {
        return user.getSubscriptionPlan() == null || user.getSubscriptionPlan() == SubscriptionPlan.FREE
                || user.getPlanExpiryDate() == null || user.getPlanExpiryDate().isBefore(LocalDate.now());
    }
    private void expireOldParticipations() { participationRepository.expireOldParticipations(LocalDateTime.now()); }
    private long countParticipants(Long sessionId) { return participationRepository.countBySessionIdAndStatusAndExpiresAtAfter(sessionId, VibeParticipationStatus.ACTIVE, LocalDateTime.now()); }
    private long secondsUntil(LocalDateTime endsAt) { return Math.max(0, Duration.between(LocalDateTime.now(), endsAt).getSeconds()); }
    private VibeEntity getVibe(Long vibeId) { return vibeRepository.findById(vibeId).orElseThrow(() -> new DataNotFoundException("Vibe not found")); }
    private UserDto getUserDetails(long userId) {
        try {
            ResponseEntity<SuccessResponseHandler<UserDto>> response = userFeingClient.getUser(userId);
            UserDto user = Optional.ofNullable(response.getBody()).map(SuccessResponseHandler::getBody).orElse(null);
            if (user == null) throw new DataNotFoundException("User not found for ID: " + userId);
            return user;
        } catch (Exception e) {
            log.error("Failed to fetch user details for userId: {}", userId, e);
            throw new DataNotFoundException("User not found or external service failed");
        }
    }
    private PreferencesDto safeGetPreference(Long userId) {
        try { return preferencesService.get(userId.intValue()); }
        catch (Exception ignored) { PreferencesDto fallback = new PreferencesDto(); fallback.setUserId(userId); fallback.setDistance(MAX_RADIUS_KM); return fallback; }
    }
    private List<UserImageEntity> getUserImages(long userId) {
        try {
            ResponseEntity<SuccessResponseHandler<List<UserImageEntity>>> response = userFeingClient.getUserImages(userId);
            return Optional.ofNullable(response.getBody()).map(SuccessResponseHandler::getBody).orElse(List.of());
        } catch (Exception e) { log.warn("Failed to fetch user images for userId: {}", userId); return List.of(); }
    }
    private void sendNotificationSafe(Long toUserId, String title, String message) {
        try {
            messagingFeingClient.sendNotification(NotificationDto.builder().toUserId(toUserId).notificationTitle(title).notificationMessage(message).build());
        } catch (Exception e) { log.warn("Unable to send notification to {}", toUserId); }
    }
    private void publishVibeEvent(String type, ActiveVibeDto activeVibe, Long userId, Long targetUserId, Map<String, Object> payload) {
        if (activeVibe == null) return;
        try {
            messagingFeingClient.broadcastVibeEvent(VibeSocketEventDto.builder().type(type).vibeId(activeVibe.getVibe().getId())
                    .sessionId(activeVibe.getSessionId()).userId(userId).targetUserId(targetUserId)
                    .participantCount(activeVibe.getParticipantCount()).remainingSeconds(activeVibe.getRemainingSeconds())
                    .payload(payload).build());
        } catch (Exception e) { log.warn("Unable to broadcast vibe event {}", type); }
    }
    private double firstNonNull(Double requested, double fallback) { return requested == null ? fallback : requested; }
    private boolean same(Object a, Object b) { return a != null && a.equals(b); }
    private void addIfPresent(List<String> badges, Enum<?> value) {
        if (value != null && !"NONE".equals(value.name()) && !"ANY".equals(value.name())) badges.add(label(value));
    }
    private String label(Enum<?> value) {
        if (value == null) return "";
        String text = value.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
    private String limit(String value, int max) { return value == null ? null : (value.length() <= max ? value : value.substring(0, max)); }
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }
}
