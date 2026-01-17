package tech.grastone.fz.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.PreferencesGayEntity;

@Repository
public interface PreferencesGayRepository extends JpaRepository<PreferencesGayEntity, Long>{

}
