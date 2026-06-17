package tech.grastone.fz.matching.dto;

import lombok.Data;
import tech.grastone.fz.matching.enums.LimitType;

@Data
public class RewardedLimitRequestDto {
    private LimitType limitType;
}
