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
import tech.grastone.fz.matching.enums.VibeConnectionStatus;

@Getter
@Setter
@Entity
@Table(name = "vibe_connections",
        indexes = {
                @Index(name = "idx_vibe_connection_user1", columnList = "user_id1"),
                @Index(name = "idx_vibe_connection_user2", columnList = "user_id2"),
                @Index(name = "idx_vibe_connection_session", columnList = "session_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vibe_connection_pair", columnNames = {"user_id1", "user_id2", "vibe_id"})
        })
public class VibeConnectionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vibe_connection_id")
    private Long vibeConnectionId;

    @Column(name = "vibe_id", nullable = false)
    private Long vibeId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id1", nullable = false)
    private Long userId1;

    @Column(name = "user_id2", nullable = false)
    private Long userId2;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VibeConnectionStatus status = VibeConnectionStatus.ACTIVE;
}
