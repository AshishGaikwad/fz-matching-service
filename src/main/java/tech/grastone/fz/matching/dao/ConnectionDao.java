package tech.grastone.fz.matching.dao;

import java.util.List;

import tech.grastone.fz.matching.entity.ConnectionsEntity;

public interface ConnectionDao {
	public ConnectionsEntity save(ConnectionsEntity connectionsEntity);

	public List<ConnectionsEntity> getConnectionByUserId1AndUserId2(Long userId1, Long userId2);
	public List<ConnectionsEntity> getConnection(Long userId);
}
