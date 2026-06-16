package tech.grastone.fz.matching.service.impl;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import tech.grastone.fz.matching.dto.BlockCheckRequestDto;
import tech.grastone.fz.matching.dto.BlockCheckResponseDto;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.SafetyService;
import tech.grastone.fz.matching.service.client.UserSafetyFeingClient;

@Service
@AllArgsConstructor
public class SafetyServiceImpl implements SafetyService {

    private final UserSafetyFeingClient userSafetyFeingClient;

    @Override
    public Set<Long> blockedUserIds(Long userId, Collection<Long> candidateUserIds) {
        if (userId == null || candidateUserIds == null || candidateUserIds.isEmpty()) {
            return Set.of();
        }

        BlockCheckRequestDto request = new BlockCheckRequestDto();
        request.setUserId(userId);
        request.setCandidateUserIds(new ArrayList<>(candidateUserIds));

        ResponseEntity<SuccessResponseHandler<BlockCheckResponseDto>> feignResponse =
                userSafetyFeingClient.checkBlockedUsers(request);
        SuccessResponseHandler<BlockCheckResponseDto> response = feignResponse == null ? null : feignResponse.getBody();
        if (response == null || response.getBody() == null || response.getBody().getBlockedUserIds() == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(response.getBody().getBlockedUserIds());
    }

    @Override
    public boolean isBlocked(Long userId, Long candidateUserId) {
        return isBlockedCandidate(userId, candidateUserId);
    }

    @Override
    public void assertNotBlocked(Long userId, Long candidateUserId) {
        if (isBlocked(userId, candidateUserId)) {
            throw new ValidationException("This user is unavailable");
        }
    }

    private boolean isBlockedCandidate(Long userId, Long candidateUserId) {
        return blockedUserIds(userId, List.of(candidateUserId)).contains(candidateUserId);
    }
}
