package tech.grastone.fz.matching.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dto.ReplyMatchRequestDto;
import tech.grastone.fz.matching.dto.SendMatchRequestDto;
import tech.grastone.fz.matching.dto.ShowProfileDto;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.MatchingV2Service;

@RestController
@RequestMapping("/match/v2")
@AllArgsConstructor
public class MatchingV2Controller {

	private final MatchingV2Service matchingV2Service;

	@GetMapping("base/{userId}")
	public ResponseEntity<SuccessResponseHandler<List<ShowProfileDto>>> getMatches(
			@PathVariable("userId") long userId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		if (page < 0)
			page = 0;
		if (size <= 0 || size > 100)
			size = 10;

		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(new SuccessResponseHandler<List<ShowProfileDto>>(200, "Matches found",
				matchingV2Service.getMatches(userId, pageable)));
	}

	@GetMapping("show/{userId}/{matchedId}")
	public ResponseEntity<SuccessResponseHandler<ShowProfileDto>> showProfile(@PathVariable("userId") long userId,
			@PathVariable("matchedId") long matchedId) {
		return ResponseEntity.ok(new SuccessResponseHandler<ShowProfileDto>(200, "Matched profile details found",
				matchingV2Service.showProfile(userId, matchedId)));
	}

	@PostMapping("request/send")
	public ResponseEntity<SuccessResponseHandler<MatchRequestEntity>> sendRequest(
			@RequestBody SendMatchRequestDto sendMatchRequestDto) {
		return ResponseEntity.ok(new SuccessResponseHandler<MatchRequestEntity>(200, "Request sent successfully",
				matchingV2Service.sendRequest(sendMatchRequestDto)));
	}

	@PostMapping("request/reply")
	public ResponseEntity<SuccessResponseHandler<MatchRequestEntity>> replyRequest(
			@RequestBody ReplyMatchRequestDto matchRequestEntity) {
		return ResponseEntity.ok(new SuccessResponseHandler<MatchRequestEntity>(200,
				matchRequestEntity.getRequestStatus() == tech.grastone.fz.matching.enums.RequestStatus.ACCEPT ? "Request accepted"
						: "Request rejected",
				matchingV2Service.replyRequest(matchRequestEntity)));
	}

	@GetMapping("request/sent/{me}")
	public ResponseEntity<SuccessResponseHandler<List<ShowProfileDto>>> sentRequest(@PathVariable long me,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		if (page < 0)
			page = 0;
		if (size <= 0 || size > 100)
			size = 10;

		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(new SuccessResponseHandler<List<ShowProfileDto>>(200, "Sent request fetch for page :" + page,
				matchingV2Service.getSentRequest(me, pageable)));
	}

	@GetMapping("request/received/{me}")
	public ResponseEntity<SuccessResponseHandler<List<ShowProfileDto>>> receivedRequest(@PathVariable long me,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		if (page < 0)
			page = 0;
		if (size <= 0 || size > 100)
			size = 10;

		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(new SuccessResponseHandler<List<ShowProfileDto>>(200,
				"Received request fetch for page :" + page, matchingV2Service.getReceivedRequest(me, pageable)));
	}
}
