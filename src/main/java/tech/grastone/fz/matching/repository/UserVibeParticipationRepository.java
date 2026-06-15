package tech.grastone.fz.matching.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.UserVibeParticipationEntity;
import tech.grastone.fz.matching.enums.VibeParticipationStatus;

@Repository
public interface UserVibeParticipationRepository extends JpaRepository<UserVibeParticipationEntity, Long> {
    List<UserVibeParticipationEntity> findByUserIdAndStatus(Long userId, VibeParticipationStatus status);

    Optional<UserVibeParticipationEntity> findFirstByUserIdAndStatusAndExpiresAtAfterOrderByJoinedAtDesc(
            Long userId,
            VibeParticipationStatus status,
            LocalDateTime now
    );

    Optional<UserVibeParticipationEntity> findByUserIdAndSessionId(Long userId, Long sessionId);

    Optional<UserVibeParticipationEntity> findByUserIdAndSessionIdAndStatusAndExpiresAtAfter(
            Long userId,
            Long sessionId,
            VibeParticipationStatus status,
            LocalDateTime now
    );

    long countBySessionIdAndStatusAndExpiresAtAfter(
            Long sessionId,
            VibeParticipationStatus status,
            LocalDateTime now
    );

    @Modifying
    @Query("""
            update UserVibeParticipationEntity p
               set p.status = tech.grastone.fz.matching.enums.VibeParticipationStatus.EXPIRED,
                   p.leftAt = :now
             where p.status = tech.grastone.fz.matching.enums.VibeParticipationStatus.ACTIVE
               and p.expiresAt <= :now
            """)
    int expireOldParticipations(@Param("now") LocalDateTime now);
}
