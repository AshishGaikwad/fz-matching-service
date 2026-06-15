package tech.grastone.fz.matching.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(
        name = "lowkey_discovery_history",
        indexes = {
                @Index(name = "idx_lowkey_history_viewer_seen", columnList = "viewer_user_id,last_seen_at"),
                @Index(name = "idx_lowkey_history_candidate", columnList = "candidate_user_id")
        }
)
public class LowkeyDiscoveryHistoryEntity extends BaseEntity {

    @EmbeddedId
    private LowkeyDiscoveryHistoryId id;

    @Column(name = "exposure_count", nullable = false)
    private Integer exposureCount;

    @Column(name = "last_score")
    private Integer lastScore;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Transient
    public Long getViewerUserId() {
        return id == null ? null : id.getViewerUserId();
    }

    public void setViewerUserId(Long viewerUserId) {
        if (id == null) {
            id = new LowkeyDiscoveryHistoryId();
        }
        id.setViewerUserId(viewerUserId);
    }

    @Transient
    public Long getCandidateUserId() {
        return id == null ? null : id.getCandidateUserId();
    }

    public void setCandidateUserId(Long candidateUserId) {
        if (id == null) {
            id = new LowkeyDiscoveryHistoryId();
        }
        id.setCandidateUserId(candidateUserId);
    }
}
