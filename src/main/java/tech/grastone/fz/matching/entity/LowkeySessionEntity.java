package tech.grastone.fz.matching.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.LowkeySessionStatus;

@Getter
@Setter
@Entity
@Table(
        name = "lowkey_sessions",
        indexes = {
                @Index(name = "idx_lowkey_user_status", columnList = "user_id,status"),
                @Index(name = "idx_lowkey_status_expiry", columnList = "status,expires_at"),
                @Index(name = "idx_lowkey_location", columnList = "latitude,longitude"),
                @Index(name = "idx_lowkey_last_seen", columnList = "last_seen_at")
        }
)
public class LowkeySessionEntity extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LowkeySessionStatus status;

    @Column(nullable = false, columnDefinition = "DECIMAL(9,6)")
    private double latitude;

    @Column(nullable = false, columnDefinition = "DECIMAL(9,6)")
    private double longitude;

    @Column(name = "location_accuracy_meters")
    private Integer locationAccuracyMeters;

    @Column(name = "radius_km", nullable = false)
    private Integer radiusKm;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "looking_for", length = 256)
    private String lookingForValues;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Transient
    public Long getSessionId() {
        return userId;
    }

    public void setSessionId(Long sessionId) {
        this.userId = sessionId;
    }

    @Transient
    public LocalDateTime getEnteredAt() {
        return getCreatedAt();
    }

    public void setEnteredAt(LocalDateTime enteredAt) {
        setCreatedAt(enteredAt);
    }
}
