package tech.grastone.fz.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.PreferencesMenEntity;

@Repository
public interface PreferencesMenRepository extends JpaRepository<PreferencesMenEntity, Long>{

}
