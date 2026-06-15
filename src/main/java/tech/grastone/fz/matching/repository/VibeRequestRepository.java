package tech.grastone.fz.matching.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.VibeRequestEntity;
import tech.grastone.fz.matching.enums.VibeRequestStatus;

@Repository
public interface VibeRequestRepository extends JpaRepository<VibeRequestEntity, Long> {
    List<VibeRequestEntity> findBySenderIdAndReceiverIdAndSessionId(Long senderId, Long receiverId, Long sessionId);

    Optional<VibeRequestEntity> findFirstBySenderIdAndReceiverIdAndSessionIdAndStatus(
            Long senderId,
            Long receiverId,
            Long sessionId,
            VibeRequestStatus status
    );
}
