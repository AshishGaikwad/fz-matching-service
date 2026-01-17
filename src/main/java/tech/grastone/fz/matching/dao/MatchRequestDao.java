package tech.grastone.fz.matching.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.enums.RequestStatus;

public interface MatchRequestDao {
	public MatchRequestEntity save(MatchRequestEntity matchRequestEntity);

	public Optional<MatchRequestEntity> get(Long id);

	public List<MatchRequestEntity> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
	public Page<MatchRequestEntity> findBySenderIdAndRequestStatus(Long senderId, RequestStatus requestStatus, Pageable page);
	public Page<MatchRequestEntity> findByReceiverIdAndRequestStatus(Long senderId, RequestStatus requestStatus, Pageable page);

}
