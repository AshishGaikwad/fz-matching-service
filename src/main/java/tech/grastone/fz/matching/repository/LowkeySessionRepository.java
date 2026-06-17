package tech.grastone.fz.matching.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.LowkeySessionEntity;
import tech.grastone.fz.matching.enums.LowkeySessionStatus;

@Repository
public interface LowkeySessionRepository extends JpaRepository<LowkeySessionEntity, Long> {

    List<LowkeySessionEntity> findByUserIdAndStatus(Long userId, LowkeySessionStatus status);

    Optional<LowkeySessionEntity> findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            LowkeySessionStatus status,
            LocalDateTime now
    );

    Optional<LowkeySessionEntity> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            LowkeySessionStatus status
    );

    long countByStatusAndExpiresAtAfter(LowkeySessionStatus status, LocalDateTime now);

    long countByStatus(LowkeySessionStatus status);

    @Query("""
            select s
              from LowkeySessionEntity s
             where s.status = tech.grastone.fz.matching.enums.LowkeySessionStatus.ACTIVE
               and s.userId <> :userId
               and s.latitude between :minLat and :maxLat
               and s.longitude between :minLon and :maxLon
             order by s.createdAt desc
            """)
    List<LowkeySessionEntity> findNearbyCandidates(
            @Param("userId") Long userId,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update LowkeySessionEntity s
               set s.status = tech.grastone.fz.matching.enums.LowkeySessionStatus.EXPIRED,
                   s.leftAt = :now
             where s.status = tech.grastone.fz.matching.enums.LowkeySessionStatus.ACTIVE
               and s.expiresAt <= :now
            """)
    int expireOldSessions(@Param("now") LocalDateTime now);
}
