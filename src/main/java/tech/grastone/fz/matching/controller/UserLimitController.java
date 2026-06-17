package tech.grastone.fz.matching.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dto.RewardedLimitRequestDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.dto.UserLimitStatusDto;
import tech.grastone.fz.matching.enums.LimitType;
import tech.grastone.fz.matching.exception.DataNotFoundException;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.UserLimitService;
import tech.grastone.fz.matching.service.client.UserFeingClient;

@RestController
@RequestMapping("/limits")
@AllArgsConstructor
public class UserLimitController {

    private final UserLimitService userLimitService;
    private final UserFeingClient userFeingClient;

    @GetMapping("{limitType}")
    public ResponseEntity<SuccessResponseHandler<UserLimitStatusDto>> status(
            Authentication authentication,
            @PathVariable LimitType limitType) {
        long userId = currentUserId(authentication);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Limit status fetched",
                userLimitService.getStatus(userId, getUser(userId), limitType)));
    }

    @PostMapping("consume")
    public ResponseEntity<SuccessResponseHandler<UserLimitStatusDto>> consume(
            Authentication authentication,
            @RequestBody RewardedLimitRequestDto request) {
        long userId = currentUserId(authentication);
        LimitType limitType = requireLimitType(request);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Limit usage recorded",
                userLimitService.consume(userId, getUser(userId), limitType)));
    }

    @PostMapping("reward")
    public ResponseEntity<SuccessResponseHandler<UserLimitStatusDto>> reward(
            Authentication authentication,
            @RequestBody RewardedLimitRequestDto request) {
        long userId = currentUserId(authentication);
        LimitType limitType = requireLimitType(request);
        return ResponseEntity.ok(new SuccessResponseHandler<>(200, "Reward applied",
                userLimitService.grantReward(userId, getUser(userId), limitType)));
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ValidationException("Authenticated user is required");
        }
        return Long.parseLong(authentication.getName());
    }

    private LimitType requireLimitType(RewardedLimitRequestDto request) {
        if (request == null || request.getLimitType() == null) {
            throw new ValidationException("Limit type is required");
        }
        return request.getLimitType();
    }

    private UserDto getUser(long userId) {
        return Optional.ofNullable(userFeingClient.getUser(userId).getBody())
                .map(SuccessResponseHandler::getBody)
                .orElseThrow(() -> new DataNotFoundException("User not found"));
    }
}
