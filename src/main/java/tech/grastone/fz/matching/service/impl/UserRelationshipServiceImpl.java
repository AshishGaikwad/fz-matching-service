package tech.grastone.fz.matching.service.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.entity.UserInteractionEventEntity;
import tech.grastone.fz.matching.entity.UserRelationshipEntity;
import tech.grastone.fz.matching.enums.RelationshipStatus;
import tech.grastone.fz.matching.enums.UserInteractionEventType;
import tech.grastone.fz.matching.enums.UserInteractionSource;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.repository.UserInteractionEventRepository;
import tech.grastone.fz.matching.repository.UserRelationshipRepository;
import tech.grastone.fz.matching.service.UserRelationshipService;

@Service
@AllArgsConstructor
@Slf4j
public class UserRelationshipServiceImpl implements UserRelationshipService {

    private static final Set<RelationshipStatus> DISCOVERY_BLOCKING_STATUSES = Set.of(
            RelationshipStatus.REQUEST_PENDING,
            RelationshipStatus.CONNECTED,
            RelationshipStatus.BLOCKED
    );

    private final UserRelationshipRepository relationshipRepository;
    private final UserInteractionEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UserRelationshipEntity sendRequest(Long senderId, Long receiverId, String message,
            UserInteractionSource source) {
        validatePair(senderId, receiverId);
        LocalDateTime now = LocalDateTime.now();
        UserRelationshipEntity relationship = getOrCreate(senderId, receiverId, source, now);

        if (relationship.getStatus() == RelationshipStatus.BLOCKED) {
            throw new ValidationException("This user is unavailable");
        }
        if (relationship.getStatus() == RelationshipStatus.CONNECTED) {
            throw new ValidationException("You are already connected");
        }
        if (relationship.getStatus() == RelationshipStatus.REQUEST_PENDING) {
            if (senderId.equals(relationship.getTargetUserId())) {
                return acceptExisting(relationship, senderId, "Auto-accepted due to mutual interest.", source, now);
            }
            throw new ValidationException("Request is already pending.");
        }

        relationship.setInitiatedBy(senderId);
        relationship.setTargetUserId(receiverId);
        relationship.setAcceptedBy(null);
        relationship.setBlockedBy(null);
        relationship.setStatus(RelationshipStatus.REQUEST_PENDING);
        relationship.setSource(source);
        relationship.setRequestMessage(message);
        relationship.setReplyMessage(null);
        relationship.setRemovedAt(null);
        relationship.setConnectedAt(null);
        touch(relationship, UserInteractionEventType.REQUEST_SENT, now);

        UserRelationshipEntity saved = relationshipRepository.save(relationship);
        recordEvent(senderId, receiverId, saved.getId(), UserInteractionEventType.REQUEST_SENT, source,
                Map.of("messagePresent", message != null && !message.isBlank()));
        return saved;
    }

    @Override
    @Transactional
    public UserRelationshipEntity acceptRequest(Long actorUserId, Long requesterUserId, String message,
            UserInteractionSource source) {
        validatePair(actorUserId, requesterUserId);
        UserRelationshipEntity relationship = find(actorUserId, requesterUserId)
                .orElseThrow(() -> new ValidationException("Request not found"));

        if (relationship.getStatus() != RelationshipStatus.REQUEST_PENDING
                || !actorUserId.equals(relationship.getTargetUserId())) {
            throw new ValidationException("This request cannot be accepted");
        }

        return acceptExisting(relationship, actorUserId, message, source, LocalDateTime.now());
    }

    @Override
    @Transactional
    public UserRelationshipEntity rejectRequest(Long actorUserId, Long requesterUserId, String message,
            UserInteractionSource source) {
        validatePair(actorUserId, requesterUserId);
        LocalDateTime now = LocalDateTime.now();
        UserRelationshipEntity relationship = find(actorUserId, requesterUserId)
                .orElseThrow(() -> new ValidationException("Request not found"));

        if (relationship.getStatus() != RelationshipStatus.REQUEST_PENDING
                || !actorUserId.equals(relationship.getTargetUserId())) {
            throw new ValidationException("This request cannot be rejected");
        }

        return rejectExisting(relationship, actorUserId, requesterUserId, message, source, now);
    }

    @Override
    @Transactional
    public UserRelationshipEntity replyRequest(Long relationshipId, boolean accepted, String message,
            UserInteractionSource source) {
        UserRelationshipEntity relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ValidationException("Request not found"));
        if (relationship.getStatus() != RelationshipStatus.REQUEST_PENDING) {
            throw new ValidationException("This request has already been answered");
        }

        Long actorUserId = relationship.getTargetUserId();
        Long requesterUserId = relationship.getInitiatedBy();
        if (accepted) {
            return acceptExisting(relationship, actorUserId, message, source, LocalDateTime.now());
        }
        return rejectExisting(relationship, actorUserId, requesterUserId, message, source, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void recordProfileViewed(Long viewerUserId, Long candidateUserId, UserInteractionSource source,
            Map<String, Object> metadata) {
        validatePair(viewerUserId, candidateUserId);
        Optional<UserRelationshipEntity> relationship = find(viewerUserId, candidateUserId);
        recordEvent(viewerUserId, candidateUserId, relationship.map(UserRelationshipEntity::getId).orElse(null),
                UserInteractionEventType.PROFILE_VIEWED, source, metadata == null ? Map.of() : metadata);
    }

    @Override
    public List<UserRelationshipEntity> getConnections(Long userId) {
        return relationshipRepository.findForUserByStatuses(userId, List.of(RelationshipStatus.CONNECTED)).stream()
                .sorted(Comparator.comparing(UserRelationshipEntity::getConnectedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<UserRelationshipEntity> getPendingSentRequests(Long userId) {
        return relationshipRepository.findByInitiatedByAndStatus(userId, RelationshipStatus.REQUEST_PENDING);
    }

    @Override
    public List<UserRelationshipEntity> getPendingReceivedRequests(Long userId) {
        return relationshipRepository.findByTargetUserIdAndStatus(userId, RelationshipStatus.REQUEST_PENDING);
    }

    @Override
    public Map<Long, UserRelationshipEntity> relationshipsForCandidates(Long userId, Collection<Long> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Map.of();
        }
        return relationshipRepository.findForUserAndCandidates(userId, candidateIds).stream()
                .collect(Collectors.toMap(
                        relationship -> relationship.otherUserId(userId),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }

    @Override
    public boolean hasBlockingState(Long userId, Long candidateUserId) {
        return find(userId, candidateUserId)
                .map(this::status)
                .map(DISCOVERY_BLOCKING_STATUSES::contains)
                .orElse(false);
    }

    @Override
    public String statusForViewer(Long viewerUserId, Long candidateUserId) {
        return find(viewerUserId, candidateUserId)
                .map(relationship -> {
                    if (relationship.getStatus() == RelationshipStatus.REQUEST_PENDING
                            && viewerUserId.equals(relationship.getTargetUserId())) {
                        return "RECEIVED";
                    }
                    return relationship.getStatus().name();
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public void recordEvent(Long actorUserId, Long targetUserId, Long relationshipId,
            UserInteractionEventType eventType, UserInteractionSource source, Map<String, Object> metadata) {
        UserInteractionEventEntity event = new UserInteractionEventEntity();
        event.setActorUserId(actorUserId);
        event.setTargetUserId(targetUserId);
        event.setRelationshipId(relationshipId);
        event.setEventType(eventType);
        event.setSource(source);
        event.setMetadataJson(toJson(metadata));
        event.setCreatedBy(actorUserId == null ? null : String.valueOf(actorUserId));
        eventRepository.save(event);
    }

    @Override
    public RelationshipStatus status(UserRelationshipEntity relationship) {
        return relationship == null ? null : relationship.getStatus();
    }

    private UserRelationshipEntity acceptExisting(UserRelationshipEntity relationship, Long actorUserId, String message,
            UserInteractionSource source, LocalDateTime now) {
        Long requesterUserId = relationship.otherUserId(actorUserId);
        relationship.setStatus(RelationshipStatus.CONNECTED);
        relationship.setAcceptedBy(actorUserId);
        relationship.setReplyMessage(message);
        relationship.setConnectedAt(now);
        relationship.setRemovedAt(null);
        relationship.setSource(source);
        touch(relationship, UserInteractionEventType.CONNECTION_CREATED, now);
        UserRelationshipEntity saved = relationshipRepository.save(relationship);
        recordEvent(actorUserId, requesterUserId, saved.getId(), UserInteractionEventType.REQUEST_ACCEPTED, source,
                Map.of());
        recordEvent(actorUserId, requesterUserId, saved.getId(), UserInteractionEventType.CONNECTION_CREATED, source,
                Map.of());
        return saved;
    }

    private UserRelationshipEntity rejectExisting(UserRelationshipEntity relationship, Long actorUserId,
            Long requesterUserId, String message, UserInteractionSource source, LocalDateTime now) {
        relationship.setStatus(RelationshipStatus.REQUEST_REJECTED);
        relationship.setReplyMessage(message);
        touch(relationship, UserInteractionEventType.REQUEST_REJECTED, now);
        UserRelationshipEntity saved = relationshipRepository.save(relationship);
        recordEvent(actorUserId, requesterUserId, saved.getId(), UserInteractionEventType.REQUEST_REJECTED, source,
                Map.of());
        return saved;
    }

    private Optional<UserRelationshipEntity> find(Long userA, Long userB) {
        Long userId1 = Math.min(userA, userB);
        Long userId2 = Math.max(userA, userB);
        return relationshipRepository.findByUserId1AndUserId2(userId1, userId2);
    }

    private UserRelationshipEntity getOrCreate(Long userA, Long userB, UserInteractionSource source,
            LocalDateTime now) {
        return find(userA, userB).orElseGet(() -> {
            UserRelationshipEntity relationship = new UserRelationshipEntity();
            relationship.setUserId1(Math.min(userA, userB));
            relationship.setUserId2(Math.max(userA, userB));
            relationship.setSource(source);
            relationship.setLastEventType(UserInteractionEventType.REQUEST_SENT);
            relationship.setLastEventAt(now);
            relationship.setCreatedBy(String.valueOf(userA));
            return relationship;
        });
    }

    private void touch(UserRelationshipEntity relationship, UserInteractionEventType eventType, LocalDateTime now) {
        relationship.setLastEventType(eventType);
        relationship.setLastEventAt(now);
        relationship.setUpdatedBy(String.valueOf(relationship.getInitiatedBy()));
    }

    private void validatePair(Long userA, Long userB) {
        if (userA == null || userB == null) {
            throw new ValidationException("Both users are required");
        }
        if (userA.equals(userB)) {
            throw new ValidationException("Cannot create relationship with yourself");
        }
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize interaction metadata keys={}", metadata.keySet());
            return "{}";
        }
    }
}
