package tech.grastone.fz.matching.service;

import tech.grastone.fz.matching.dto.ShowProfileDto;
import tech.grastone.fz.matching.entity.ConnectionsEntity;

import java.util.List;

public interface ConnectionService {
	public List<ShowProfileDto> getConnections(long userId);
}
