package tech.grastone.fz.matching.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.enums.RequestStatus;

import java.util.List;


@Repository
public interface MatchRequestRepository extends JpaRepository<MatchRequestEntity, Long> {
	List<MatchRequestEntity> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
	Page<MatchRequestEntity> findBySenderIdAndRequestStatus(Long senderId, RequestStatus requestStatus, Pageable page);
	Page<MatchRequestEntity> findByReceiverIdAndRequestStatus(Long receiverId, RequestStatus requestStatus, Pageable page);

}
