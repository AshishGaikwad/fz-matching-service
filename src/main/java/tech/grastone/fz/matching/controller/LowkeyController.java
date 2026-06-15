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
import tech.grastone.fz.matching.dto.LowkeyDiscoverDto;
import tech.grastone.fz.matching.dto.LowkeyEnterRequestDto;
import tech.grastone.fz.matching.dto.LowkeyLeaveRequestDto;
import tech.grastone.fz.matching.dto.LowkeyLocationUpdateRequestDto;
import tech.grastone.fz.matching.dto.LowkeyRequestDto;
import tech.grastone.fz.matching.dto.LowkeySessionDto;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.LowkeyService;

@RestController
@RequestMapping("/lowkey")
@Slf4j
@AllArgsConstructor
public class LowkeyController {

    private final LowkeyService lowkeyService;

    @GetMapping("/me")
    public ResponseEntity<SuccessResponseHandler<LowkeySessionDto>> getMySession(Authentication authentication) {
        Long userId = authenticatedUserId(authentication, null);
        log.info("[Lowkey] GET /me requested by userId={}", userId);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Lowkey session fetched",
                lowkeyService.getMySession(userId)));
    }

    @PostMapping("/enter")
    public ResponseEntity<SuccessResponseHandler<LowkeySessionDto>> enter(
            Authentication authentication,
            @RequestBody LowkeyEnterRequestDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getUserId());
        log.info("[Lowkey] POST /enter userId={} authPrincipal={} request={}", userId,
                authentication == null ? null : authentication.getName(), request);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Entered Lowkey",
                lowkeyService.enter(userId, request)));
    }

    @PostMapping("/location")
    public ResponseEntity<SuccessResponseHandler<LowkeySessionDto>> updateLocation(
            Authentication authentication,
            @RequestBody LowkeyLocationUpdateRequestDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getUserId());
        log.info("[Lowkey] POST /location userId={} authPrincipal={} request={}", userId,
                authentication == null ? null : authentication.getName(), request);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Lowkey location updated",
                lowkeyService.updateLocation(userId, request)));
    }

    @PostMapping("/leave")
    public ResponseEntity<SuccessResponseHandler<LowkeySessionDto>> leave(
            Authentication authentication,
            @RequestBody LowkeyLeaveRequestDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getUserId());
        log.info("[Lowkey] POST /leave userId={} authPrincipal={} request={}", userId,
                authentication == null ? null : authentication.getName(), request);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Left Lowkey",
                lowkeyService.leave(userId, request)));
    }

    @GetMapping("/discover")
    public ResponseEntity<SuccessResponseHandler<List<LowkeyDiscoverDto>>> discover(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = authenticatedUserId(authentication, null);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)));
        log.info("[Lowkey] GET /discover userId={} authPrincipal={} page={} size={}", userId,
                authentication == null ? null : authentication.getName(), page, size);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Lowkey matches discovered",
                lowkeyService.discover(userId, pageable)));
    }

    @PostMapping("/request")
    public ResponseEntity<SuccessResponseHandler<MatchRequestEntity>> sendRequest(
            Authentication authentication,
            @RequestBody LowkeyRequestDto request) {
        Long userId = authenticatedUserId(authentication, request == null ? null : request.getSenderId());
        log.info("[Lowkey] POST /request senderId={} authPrincipal={} request={}", userId,
                authentication == null ? null : authentication.getName(), request);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Lowkey request sent",
                lowkeyService.sendRequest(userId, request)));
    }

    private Long authenticatedUserId(Authentication authentication, Long fallbackUserId) {
        if (authentication != null && authentication.getPrincipal() != null) {
            Long parsedUserId = Long.parseLong(authentication.getPrincipal().toString());
            log.debug("[Lowkey] authenticatedUserId resolved from security context: {}", parsedUserId);
            return parsedUserId;
        }
        log.debug("[Lowkey] authenticatedUserId falling back to request body value: {}", fallbackUserId);
        return fallbackUserId;
    }
}
