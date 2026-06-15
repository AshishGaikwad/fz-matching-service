package tech.grastone.fz.matching.dao;

import java.util.List;

import org.springframework.data.domain.Pageable;

import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.dto.VibeCandidateRowDto;

public interface VibeDiscoveryDao {
    List<VibeCandidateRowDto> discoverCandidates(UserDto currentUser, Long sessionId, double latitude,
            double longitude, int radiusKm, Pageable pageable);
}
