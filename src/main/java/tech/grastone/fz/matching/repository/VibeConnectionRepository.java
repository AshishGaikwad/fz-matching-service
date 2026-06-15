package tech.grastone.fz.matching.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.VibeConnectionEntity;

@Repository
public interface VibeConnectionRepository extends JpaRepository<VibeConnectionEntity, Long> {
    Optional<VibeConnectionEntity> findByUserId1AndUserId2AndVibeId(Long userId1, Long userId2, Long vibeId);
}
