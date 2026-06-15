package tech.grastone.fz.matching.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.VibeEntity;

@Repository
public interface VibeRepository extends JpaRepository<VibeEntity, Long> {
    List<VibeEntity> findByActiveTrueOrderBySortOrderAsc();
    Optional<VibeEntity> findByVibeIdAndActiveTrue(Long vibeId);
    boolean existsByCode(String code);
}
