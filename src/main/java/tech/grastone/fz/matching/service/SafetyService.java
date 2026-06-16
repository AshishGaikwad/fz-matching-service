package tech.grastone.fz.matching.service;

import java.util.Collection;
import java.util.Set;

public interface SafetyService {
    Set<Long> blockedUserIds(Long userId, Collection<Long> candidateUserIds);
    boolean isBlocked(Long userId, Long candidateUserId);
    void assertNotBlocked(Long userId, Long candidateUserId);
}
