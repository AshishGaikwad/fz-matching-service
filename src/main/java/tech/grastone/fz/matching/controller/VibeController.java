package tech.grastone.fz.matching.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dto.ActiveVibeDto;
import tech.grastone.fz.matching.dto.JoinVibeRequestDto;
import tech.grastone.fz.matching.dto.LeaveVibeRequestDto;
import tech.grastone.fz.matching.dto.VibeDiscoverDto;
import tech.grastone.fz.matching.dto.VibeDto;
import tech.grastone.fz.matching.dto.VibeRequestDto;
import tech.grastone.fz.matching.dto.VibeRequestReplyDto;
import tech.grastone.fz.matching.entity.VibeRequestEntity;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.VibeService;

@RestController
@RequestMapping("/vibes")
@Slf4j
@AllArgsConstructor
public class VibeController {

    private final VibeService vibeService;

    @GetMapping
    public ResponseEntity<SuccessResponseHandler<List<VibeDto>>> getVibes() {
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Vibes fetched", vibeService.getVibes()));
    }

    @GetMapping("/me")
    public ResponseEntity<SuccessResponseHandler<ActiveVibeDto>> getMyActiveVibe(Authentication authentication) {
        Long userId = authenticatedUserId(authentication, null);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Active vibe fetched", vibeService.getMyActiveVibe(userId)));
    }

    @GetMapping("/nearby")
    public ResponseEntity<SuccessResponseHandler<List<VibeDto>>> getNearbyVibes(
            Authentication authentication,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Integer radiusKm) {
        Long userId = authenticatedUserId(authentication, null);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Nearby vibes fetched",
                vibeService.getNearbyVibes(userId, latitude, longitude, radiusKm)));
    }

    @PostMapping("/join")
    public ResponseEntity<SuccessResponseHandler<ActiveVibeDto>> joinVibe(
            Authentication authentication,
            @RequestBody JoinVibeRequestDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getUserId());
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Joined vibe", vibeService.joinVibe(userId, request)));
    }

    @PostMapping("/leave")
    public ResponseEntity<SuccessResponseHandler<ActiveVibeDto>> leaveVibe(
            Authentication authentication,
            @RequestBody LeaveVibeRequestDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getUserId());
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Left vibe", vibeService.leaveVibe(userId, request)));
    }

    @GetMapping("/discover")
    public ResponseEntity<SuccessResponseHandler<List<VibeDiscoverDto>>> discover(
            Authentication authentication,
            @RequestParam(required = false) Long vibeId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = authenticatedUserId(authentication, null);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)));
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Vibers discovered",
                vibeService.discover(userId, vibeId, sessionId, pageable)));
    }

    @PostMapping("/request")
    public ResponseEntity<SuccessResponseHandler<VibeRequestEntity>> sendRequest(
            Authentication authentication,
            @RequestBody VibeRequestDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getSenderId());
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Vibe request sent",
                vibeService.sendRequest(userId, request)));
    }

    @PostMapping("/request/accept")
    public ResponseEntity<SuccessResponseHandler<VibeRequestEntity>> acceptRequest(
            Authentication authentication,
            @RequestBody VibeRequestReplyDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getUserId());
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Vibe request accepted",
                vibeService.acceptRequest(userId, request)));
    }

    @PostMapping("/request/reject")
    public ResponseEntity<SuccessResponseHandler<VibeRequestEntity>> rejectRequest(
            Authentication authentication,
            @RequestBody VibeRequestReplyDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getUserId());
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Vibe request rejected",
                vibeService.rejectRequest(userId, request)));
    }

    private Long authenticatedUserId(Authentication authentication, Long fallbackUserId) {
        if (authentication != null && authentication.getPrincipal() != null) {
            return Long.parseLong(authentication.getPrincipal().toString());
        }
        return fallbackUserId;
    }
}
