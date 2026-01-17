package tech.grastone.fz.matching.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import tech.grastone.fz.matching.dto.ReplyMatchRequestDto;
import tech.grastone.fz.matching.dto.SendMatchRequestDto;
import tech.grastone.fz.matching.dto.ShowProfileDto;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.entity.UserMatchesEntity;

public interface MatchingService {

	public List<ShowProfileDto> getMatches(long userId, Pageable page);

	public ShowProfileDto showProfile(long userId, long matchedUserId);

	public MatchRequestEntity sendRequest(SendMatchRequestDto sendMatchRequestDto);

	public MatchRequestEntity replyRequest(ReplyMatchRequestDto replyMatchRequestDto);

	public List<ShowProfileDto>  getSentRequest(long userId, Pageable page);

	public List<ShowProfileDto>  getReceivedRequest(long userId, Pageable page);
}
