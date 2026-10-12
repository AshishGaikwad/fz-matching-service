package tech.grastone.fz.matching.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import tech.grastone.fz.matching.entity.UserRelationshipEntity;
import tech.grastone.fz.matching.enums.RelationshipStatus;
import tech.grastone.fz.matching.enums.UserInteractionEventType;
import tech.grastone.fz.matching.enums.UserInteractionSource;

public interface UserRelationshipService {

    UserRelationshipEntity sendRequest(Long senderId, Long receiverId, String message, UserInteractionSource source);

    UserRelationshipEntity acceptRequest(Long actorUserId, Long requesterUserId, String message,
            UserInteractionSource source);

    UserRelationshipEntity rejectRequest(Long actorUserId, Long requesterUserId, String message,
            UserInteractionSource source);

    UserRelationshipEntity replyRequest(Long relationshipId, boolean accepted, String message,
            UserInteractionSource source);

    void recordProfileViewed(Long viewerUserId, Long candidateUserId, UserInteractionSource source,
            Map<String, Object> metadata);

    List<UserRelationshipEntity> getConnections(Long userId);

    List<UserRelationshipEntity> getPendingSentRequests(Long userId);

    List<UserRelationshipEntity> getPendingReceivedRequests(Long userId);

    Map<Long, UserRelationshipEntity> relationshipsForCandidates(Long userId, Collection<Long> candidateIds);

    boolean hasBlockingState(Long userId, Long candidateUserId);

    String statusForViewer(Long viewerUserId, Long candidateUserId);

    void recordEvent(Long actorUserId, Long targetUserId, Long relationshipId, UserInteractionEventType eventType,
            UserInteractionSource source, Map<String, Object> metadata);

    RelationshipStatus status(UserRelationshipEntity relationship);
}
