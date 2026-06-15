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
        name = "match_score_cache",
        indexes = {
                @Index(name = "idx_match_score_viewer", columnList = "viewer_user_id,expires_at"),
                @Index(name = "idx_match_score_candidate", columnList = "candidate_user_id")
        }
)
public class MatchScoreCacheEntity extends BaseEntity {

    @EmbeddedId
    private MatchScoreCacheId id;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "match_grade", nullable = false, length = 4)
    private String matchGrade;

    @Column(name = "match_explanation", length = 512)
    private String matchExplanation;

    @Column(name = "score_breakdown", columnDefinition = "TEXT")
    private String scoreBreakdown;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Transient
    public Long getViewerUserId() {
        return id == null ? null : id.getViewerUserId();
    }

    public void setViewerUserId(Long viewerUserId) {
        if (id == null) {
            id = new MatchScoreCacheId();
        }
        id.setViewerUserId(viewerUserId);
    }

    @Transient
    public Long getCandidateUserId() {
        return id == null ? null : id.getCandidateUserId();
    }

    public void setCandidateUserId(Long candidateUserId) {
        if (id == null) {
            id = new MatchScoreCacheId();
        }
        id.setCandidateUserId(candidateUserId);
    }
}
