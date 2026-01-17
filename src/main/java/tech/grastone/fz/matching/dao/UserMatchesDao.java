package tech.grastone.fz.matching.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import tech.grastone.fz.matching.entity.UserMatchesEntity;
import tech.grastone.fz.matching.enums.MatchStatus;
import tech.grastone.fz.matching.enums.MatchType;

public interface UserMatchesDao {

	public Page<UserMatchesEntity> findByUserIdAndStatusAndType(Long uid, MatchStatus matchStatus, MatchType matchType,Pageable pageable);
	
	public List<UserMatchesEntity> saveAll(List<UserMatchesEntity> userMatchesEntities);
	
	public UserMatchesEntity getById(Long userMtachesId);
	
	public UserMatchesEntity save(UserMatchesEntity userMatchesEntity);

}
