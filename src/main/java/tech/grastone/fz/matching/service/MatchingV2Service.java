package tech.grastone.fz.matching.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import tech.grastone.fz.matching.dto.ReplyMatchRequestDto;
import tech.grastone.fz.matching.dto.SendMatchRequestDto;
import tech.grastone.fz.matching.dto.ShowProfileDto;
import tech.grastone.fz.matching.entity.MatchRequestEntity;

public interface MatchingV2Service {
	List<ShowProfileDto> getMatches(long userId, Pageable pageable);

	ShowProfileDto showProfile(long userId, long matchedUserId);

	MatchRequestEntity sendRequest(SendMatchRequestDto sendMatchRequestDto);

	MatchRequestEntity replyRequest(ReplyMatchRequestDto replyMatchRequestDto);

	List<ShowProfileDto> getSentRequest(long userId, Pageable pageable);

	List<ShowProfileDto> getReceivedRequest(long userId, Pageable pageable);
}
