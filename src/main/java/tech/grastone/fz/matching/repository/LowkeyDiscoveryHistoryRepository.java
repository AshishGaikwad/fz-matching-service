package tech.grastone.fz.matching.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.LowkeyDiscoveryHistoryEntity;
import tech.grastone.fz.matching.entity.LowkeyDiscoveryHistoryId;

@Repository
public interface LowkeyDiscoveryHistoryRepository extends JpaRepository<LowkeyDiscoveryHistoryEntity, LowkeyDiscoveryHistoryId> {
    Optional<LowkeyDiscoveryHistoryEntity> findByIdViewerUserIdAndIdCandidateUserId(
            Long viewerUserId,
            Long candidateUserId
    );

    List<LowkeyDiscoveryHistoryEntity> findByIdViewerUserIdAndIdCandidateUserIdIn(
            Long viewerUserId,
            Collection<Long> candidateUserIds
    );
}
