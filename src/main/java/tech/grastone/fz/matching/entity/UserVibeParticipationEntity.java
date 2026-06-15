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
import tech.grastone.fz.matching.enums.VibeParticipationStatus;

@Getter
@Setter
@Entity
@Table(name = "user_vibe_participation",
        indexes = {
                @Index(name = "idx_vibe_participation_user_status", columnList = "user_id,status"),
                @Index(name = "idx_vibe_participation_session_status", columnList = "session_id,status"),
                @Index(name = "idx_vibe_participation_location", columnList = "latitude,longitude"),
                @Index(name = "idx_vibe_participation_expiry", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_vibe_session", columnNames = {"user_id", "session_id"})
        })
public class UserVibeParticipationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participation_id")
    private Long participationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "vibe_id", nullable = false)
    private Long vibeId;

    @Column(columnDefinition = "DECIMAL(9,6)", nullable = false)
    private double latitude;

    @Column(columnDefinition = "DECIMAL(9,6)", nullable = false)
    private double longitude;

    @Column(name = "radius_km", nullable = false)
    private int radiusKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VibeParticipationStatus status = VibeParticipationStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
