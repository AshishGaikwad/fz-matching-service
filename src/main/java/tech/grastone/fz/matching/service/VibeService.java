package tech.grastone.fz.matching.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import tech.grastone.fz.matching.dto.ActiveVibeDto;
import tech.grastone.fz.matching.dto.JoinVibeRequestDto;
import tech.grastone.fz.matching.dto.LeaveVibeRequestDto;
import tech.grastone.fz.matching.dto.VibeDiscoverDto;
import tech.grastone.fz.matching.dto.VibeDto;
import tech.grastone.fz.matching.dto.VibeRequestDto;
import tech.grastone.fz.matching.dto.VibeRequestReplyDto;
import tech.grastone.fz.matching.entity.VibeRequestEntity;

public interface VibeService {
    List<VibeDto> getVibes();
    List<VibeDto> getNearbyVibes(Long userId, Double latitude, Double longitude, Integer radiusKm);
    ActiveVibeDto getMyActiveVibe(Long userId);
    ActiveVibeDto joinVibe(Long userId, JoinVibeRequestDto request);
    ActiveVibeDto leaveVibe(Long userId, LeaveVibeRequestDto request);
    List<VibeDiscoverDto> discover(Long userId, Long vibeId, Long sessionId, Pageable pageable);
    VibeRequestEntity sendRequest(Long userId, VibeRequestDto request);
    VibeRequestEntity acceptRequest(Long userId, VibeRequestReplyDto request);
    VibeRequestEntity rejectRequest(Long userId, VibeRequestReplyDto request);
}
