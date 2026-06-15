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
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.VibeSessionStatus;

@Getter
@Setter
@Entity
@Table(name = "active_vibe_sessions", indexes = {
        @Index(name = "idx_active_vibe_status_end", columnList = "vibe_id,status,ends_at"),
        @Index(name = "idx_active_vibe_ends_at", columnList = "ends_at")
})
public class ActiveVibeSessionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "vibe_id", nullable = false)
    private Long vibeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VibeSessionStatus status = VibeSessionStatus.ACTIVE;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;
}
