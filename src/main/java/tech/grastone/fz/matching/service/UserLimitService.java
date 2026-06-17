package tech.grastone.fz.matching.service;

import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.dto.UserLimitStatusDto;
import tech.grastone.fz.matching.enums.LimitType;

public interface UserLimitService {
    UserLimitStatusDto getStatus(long userId, UserDto user, LimitType limitType);
    UserLimitStatusDto consume(long userId, UserDto user, LimitType limitType);
    UserLimitStatusDto grantReward(long userId, UserDto user, LimitType limitType);
}
