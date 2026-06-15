package tech.grastone.fz.matching.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import tech.grastone.fz.matching.dto.LowkeyDiscoverDto;
import tech.grastone.fz.matching.dto.LowkeyEnterRequestDto;
import tech.grastone.fz.matching.dto.LowkeyLeaveRequestDto;
import tech.grastone.fz.matching.dto.LowkeyLocationUpdateRequestDto;
import tech.grastone.fz.matching.dto.LowkeyRequestDto;
import tech.grastone.fz.matching.dto.LowkeySessionDto;
import tech.grastone.fz.matching.entity.MatchRequestEntity;

public interface LowkeyService {
    LowkeySessionDto getMySession(Long userId);
    LowkeySessionDto enter(Long userId, LowkeyEnterRequestDto request);
    LowkeySessionDto updateLocation(Long userId, LowkeyLocationUpdateRequestDto request);
    LowkeySessionDto leave(Long userId, LowkeyLeaveRequestDto request);
    List<LowkeyDiscoverDto> discover(Long userId, Pageable pageable);
    MatchRequestEntity sendRequest(Long userId, LowkeyRequestDto request);
}
