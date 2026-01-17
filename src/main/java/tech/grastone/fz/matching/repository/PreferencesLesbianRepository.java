package tech.grastone.fz.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.PreferencesLesbianEntity;

@Repository
public interface PreferencesLesbianRepository extends JpaRepository<PreferencesLesbianEntity, Long>{

}
