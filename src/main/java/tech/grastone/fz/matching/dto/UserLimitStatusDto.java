package tech.grastone.fz.matching.dto;

import lombok.Builder;
import lombok.Data;
import tech.grastone.fz.matching.enums.LimitType;

@Data
@Builder
public class UserLimitStatusDto {
    private LimitType limitType;
    private int usageCount;
    private int limitValue;
    private int rewardedCount;
    private int maxRewardedCount;
    private boolean premium;
    private boolean allowed;
}
