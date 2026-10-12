package tech.grastone.fz.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.UserInteractionEventType;
import tech.grastone.fz.matching.enums.UserInteractionSource;

@Getter
@Setter
@Entity
@Table(
        name = "user_interaction_events",
        indexes = {
                @Index(name = "idx_interaction_actor_created", columnList = "actor_user_id,created_at"),
                @Index(name = "idx_interaction_target_created", columnList = "target_user_id,created_at"),
                @Index(name = "idx_interaction_relationship", columnList = "relationship_id"),
                @Index(name = "idx_interaction_type_created", columnList = "event_type,created_at")
        }
)
public class UserInteractionEventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "relationship_id")
    private Long relationshipId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private UserInteractionEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserInteractionSource source;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
