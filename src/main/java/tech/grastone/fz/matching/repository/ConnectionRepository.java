package tech.grastone.fz.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.ConnectionsEntity;
import java.util.List;


@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionsEntity, Long> {

	List<ConnectionsEntity> findByUserId1AndUserId2(Long userId1, Long userId2);
	List<ConnectionsEntity> findByUserId1OrUserId2(Long userId1, Long userId2);
}
