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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.VibeRequestStatus;

@Getter
@Setter
@Entity
@Table(name = "vibe_requests",
        indexes = {
                @Index(name = "idx_vibe_request_sender", columnList = "sender_id,status"),
                @Index(name = "idx_vibe_request_receiver", columnList = "receiver_id,status"),
                @Index(name = "idx_vibe_request_session", columnList = "session_id,status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vibe_request_pair_session", columnNames = {"sender_id", "receiver_id", "session_id"})
        })
public class VibeRequestEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vibe_request_id")
    private Long vibeRequestId;

    @Column(name = "vibe_id", nullable = false)
    private Long vibeId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "request_message", length = 160)
    private String requestMessage;

    @Column(name = "response_message", length = 160)
    private String responseMessage;

    @Column(name = "compatibility_score", nullable = false)
    private int compatibilityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VibeRequestStatus status = VibeRequestStatus.PENDING;
}
