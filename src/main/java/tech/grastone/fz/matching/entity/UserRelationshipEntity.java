package tech.grastone.fz.matching.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.RelationshipStatus;
import tech.grastone.fz.matching.enums.UserInteractionEventType;
import tech.grastone.fz.matching.enums.UserInteractionSource;

@Getter
@Setter
@Entity
@Table(
        name = "user_relationships",
        indexes = {
                @Index(name = "idx_relationship_user_1", columnList = "user_id_1,status"),
                @Index(name = "idx_relationship_user_2", columnList = "user_id_2,status"),
                @Index(name = "idx_relationship_pair", columnList = "user_id_1,user_id_2")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_relationship_pair", columnNames = { "user_id_1", "user_id_2" })
        }
)
public class UserRelationshipEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id_1", nullable = false)
    private Long userId1;

    @Column(name = "user_id_2", nullable = false)
    private Long userId2;

    @Column(name = "initiated_by")
    private Long initiatedBy;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "accepted_by")
    private Long acceptedBy;

    @Column(name = "blocked_by")
    private Long blockedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RelationshipStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private UserInteractionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_event_type", nullable = false, length = 32)
    private UserInteractionEventType lastEventType;

    @Column(name = "last_event_at", nullable = false)
    private LocalDateTime lastEventAt;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "request_message", length = 100)
    private String requestMessage;

    @Column(name = "reply_message", length = 100)
    private String replyMessage;

    public Long otherUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        if (userId.equals(userId1)) {
            return userId2;
        }
        if (userId.equals(userId2)) {
            return userId1;
        }
        return null;
    }
}
