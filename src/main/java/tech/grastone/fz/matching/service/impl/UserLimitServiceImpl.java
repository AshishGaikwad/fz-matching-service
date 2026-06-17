package tech.grastone.fz.matching.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.UserLimitsDao;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.dto.UserLimitStatusDto;
import tech.grastone.fz.matching.entity.UserLimitsEntity;
import tech.grastone.fz.matching.enums.Frequency;
import tech.grastone.fz.matching.enums.LimitType;
import tech.grastone.fz.matching.enums.SubscriptionPlan;
import tech.grastone.fz.matching.exception.DataLimitException;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.service.UserLimitService;
import tech.grastone.fz.matching.util.CommonUtil;

@Service
@AllArgsConstructor
public class UserLimitServiceImpl implements UserLimitService {

    private static final int FREE_PROFILE_ACTION_BASE_LIMIT = 20;
    private static final int FREE_PROFILE_ACTION_MAX_LIMIT = 60;
    private static final int FREE_PROFILE_ACTION_MAX_REWARDS = 3;
    private static final int FREE_VIBE_JOIN_BASE_LIMIT = 1;
    private static final int FREE_VIBE_JOIN_MAX_LIMIT = 3;
    private static final int FREE_VIBE_JOIN_MAX_REWARDS = 2;
    private static final int FREE_SEEN_YOU_BASE_LIMIT = 1;
    private static final int FREE_SEEN_YOU_MAX_LIMIT = 3;
    private static final int FREE_SEEN_YOU_MAX_REWARDS = 2;

    private final UserLimitsDao limitsDao;
    private final CommonUtil commonUtil;

    @Override
    @Transactional
    public UserLimitStatusDto getStatus(long userId, UserDto user, LimitType limitType) {
        if (!isLimited(limitType)) {
            throw new ValidationException("Unsupported limit type");
        }
        if (!isFreeUser(user)) {
            return premiumStatus(limitType);
        }
        UserLimitsEntity usage = getOrCreate(userId, limitType, false);
        UserLimitsEntity rewards = getOrCreate(userId, rewardLimitType(limitType), true);
        return toStatus(limitType, usage, rewards, false);
    }

    @Override
    @Transactional
    public UserLimitStatusDto consume(long userId, UserDto user, LimitType limitType) {
        if (!isLimited(limitType)) {
            throw new ValidationException("Unsupported limit type");
        }
        if (!isFreeUser(user)) {
            return premiumStatus(limitType);
        }
        UserLimitsEntity usage = getOrCreate(userId, limitType, false);
        UserLimitsEntity rewards = getOrCreate(userId, rewardLimitType(limitType), true);
        if (usage.getUsageCount() >= usage.getLimitValue()) {
            throw new DataLimitException("Daily free limit reached");
        }
        usage.setUsageCount(usage.getUsageCount() + 1);
        usage = limitsDao.save(usage);
        return toStatus(limitType, usage, rewards, false);
    }

    @Override
    @Transactional
    public UserLimitStatusDto grantReward(long userId, UserDto user, LimitType limitType) {
        if (!isLimited(limitType)) {
            throw new ValidationException("Unsupported limit type");
        }
        if (!isFreeUser(user)) {
            return premiumStatus(limitType);
        }
        UserLimitsEntity usage = getOrCreate(userId, limitType, false);
        UserLimitsEntity rewards = getOrCreate(userId, rewardLimitType(limitType), true);
        int maxRewards = maxRewardCount(limitType);
        if (rewards.getUsageCount() >= maxRewards) {
            throw new DataLimitException("Rewarded ad limit reached for today");
        }
        rewards.setUsageCount(rewards.getUsageCount() + 1);
        rewards = limitsDao.save(rewards);
        usage.setLimitValue(limitValueForRewardCount(limitType, rewards.getUsageCount()));
        usage = limitsDao.save(usage);
        return toStatus(limitType, usage, rewards, false);
    }

    private UserLimitsEntity getOrCreate(long userId, LimitType limitType, boolean rewardCounter) {
        String periodKey = commonUtil.getPeriod(Frequency.DAILY);
        UserLimitsEntity limits = limitsDao.getUserLimits(userId, Frequency.DAILY, limitType, periodKey);
        if (limits != null) {
            return limits;
        }
        limits = new UserLimitsEntity();
        limits.setUserId(userId);
        limits.setFrequency(Frequency.DAILY);
        limits.setLimitType(limitType);
        limits.setPeriodKey(periodKey);
        limits.setUsageCount(0);
        limits.setLimitValue(rewardCounter ? maxRewardCount(originalLimitType(limitType)) : baseLimit(limitType));
        return limitsDao.save(limits);
    }

    private UserLimitStatusDto toStatus(LimitType limitType, UserLimitsEntity usage, UserLimitsEntity rewards, boolean premium) {
        return UserLimitStatusDto.builder()
                .limitType(limitType)
                .usageCount(usage.getUsageCount())
                .limitValue(usage.getLimitValue())
                .rewardedCount(rewards.getUsageCount())
                .maxRewardedCount(maxRewardCount(limitType))
                .premium(premium)
                .allowed(usage.getUsageCount() < usage.getLimitValue())
                .build();
    }

    private UserLimitStatusDto premiumStatus(LimitType limitType) {
        return UserLimitStatusDto.builder()
                .limitType(limitType)
                .usageCount(0)
                .limitValue(Integer.MAX_VALUE)
                .rewardedCount(0)
                .maxRewardedCount(0)
                .premium(true)
                .allowed(true)
                .build();
    }

    private int baseLimit(LimitType limitType) {
        return switch (limitType) {
            case VIBE_PROFILE_ACTION -> FREE_PROFILE_ACTION_BASE_LIMIT;
            case VIBE_JOIN -> FREE_VIBE_JOIN_BASE_LIMIT;
            case SEEN_YOU_VIEW -> FREE_SEEN_YOU_BASE_LIMIT;
            default -> throw new ValidationException("Unsupported limit type");
        };
    }

    private int maxRewardCount(LimitType limitType) {
        return switch (originalLimitType(limitType)) {
            case VIBE_PROFILE_ACTION -> FREE_PROFILE_ACTION_MAX_REWARDS;
            case VIBE_JOIN -> FREE_VIBE_JOIN_MAX_REWARDS;
            case SEEN_YOU_VIEW -> FREE_SEEN_YOU_MAX_REWARDS;
            default -> 0;
        };
    }

    private int limitValueForRewardCount(LimitType limitType, int rewardCount) {
        return switch (limitType) {
            case VIBE_PROFILE_ACTION -> Math.min(FREE_PROFILE_ACTION_MAX_LIMIT,
                    FREE_PROFILE_ACTION_BASE_LIMIT + (int) Math.ceil((FREE_PROFILE_ACTION_MAX_LIMIT - FREE_PROFILE_ACTION_BASE_LIMIT) * (rewardCount / 3.0)));
            case VIBE_JOIN -> Math.min(FREE_VIBE_JOIN_MAX_LIMIT, FREE_VIBE_JOIN_BASE_LIMIT + rewardCount);
            case SEEN_YOU_VIEW -> Math.min(FREE_SEEN_YOU_MAX_LIMIT, FREE_SEEN_YOU_BASE_LIMIT + rewardCount);
            default -> throw new ValidationException("Unsupported limit type");
        };
    }

    private boolean isLimited(LimitType limitType) {
        return limitType == LimitType.VIBE_PROFILE_ACTION || limitType == LimitType.VIBE_JOIN || limitType == LimitType.SEEN_YOU_VIEW;
    }

    private LimitType rewardLimitType(LimitType limitType) {
        return switch (limitType) {
            case VIBE_PROFILE_ACTION -> LimitType.REWARDED_VIBE_PROFILE_ACTION;
            case VIBE_JOIN -> LimitType.REWARDED_VIBE_JOIN;
            case SEEN_YOU_VIEW -> LimitType.REWARDED_SEEN_YOU_VIEW;
            default -> throw new ValidationException("Unsupported limit type");
        };
    }

    private LimitType originalLimitType(LimitType limitType) {
        if (limitType == LimitType.REWARDED_VIBE_PROFILE_ACTION) return LimitType.VIBE_PROFILE_ACTION;
        if (limitType == LimitType.REWARDED_VIBE_JOIN) return LimitType.VIBE_JOIN;
        if (limitType == LimitType.REWARDED_SEEN_YOU_VIEW) return LimitType.SEEN_YOU_VIEW;
        return limitType;
    }

    private boolean isFreeUser(UserDto user) {
        return user.getSubscriptionPlan() == null
                || user.getSubscriptionPlan() == SubscriptionPlan.FREE
                || user.getPlanExpiryDate() == null
                || user.getPlanExpiryDate().isBefore(LocalDate.now());
    }
}
