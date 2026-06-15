package tech.grastone.fz.matching.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.ActiveVibeSessionEntity;
import tech.grastone.fz.matching.enums.VibeSessionStatus;

@Repository
public interface ActiveVibeSessionRepository extends JpaRepository<ActiveVibeSessionEntity, Long> {
    Optional<ActiveVibeSessionEntity> findFirstByVibeIdAndStatusAndEndsAtAfterOrderByEndsAtAsc(
            Long vibeId,
            VibeSessionStatus status,
            LocalDateTime now
    );
}
