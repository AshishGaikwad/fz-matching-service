package tech.grastone.fz.matching.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.MatchScoreCacheEntity;
import tech.grastone.fz.matching.entity.MatchScoreCacheId;

@Repository
public interface MatchScoreCacheRepository extends JpaRepository<MatchScoreCacheEntity, MatchScoreCacheId> {
    Optional<MatchScoreCacheEntity> findByIdViewerUserIdAndIdCandidateUserId(
            Long viewerUserId,
            Long candidateUserId
    );

    Optional<MatchScoreCacheEntity> findByIdViewerUserIdAndIdCandidateUserIdAndExpiresAtAfter(
            Long viewerUserId,
            Long candidateUserId,
            LocalDateTime now
    );
}
