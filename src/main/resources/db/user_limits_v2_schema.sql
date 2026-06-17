-- User limit enum expansion for AdMob/rewarded-ad gates.
-- Hibernate generated the original MySQL enum ordinal check when LimitType had 3 values.
-- New LimitType ordinals:
-- 0 PROFILE_VIEW
-- 1 REQUEST_SENT
-- 2 CONNECTION_ACCEPTED
-- 3 VIBE_PROFILE_ACTION
-- 4 VIBE_JOIN
-- 5 SEEN_YOU_VIEW
-- 6 REWARDED_VIBE_PROFILE_ACTION
-- 7 REWARDED_VIBE_JOIN
-- 8 REWARDED_SEEN_YOU_VIEW

ALTER TABLE user_limits DROP CHECK user_limits_chk_2;

ALTER TABLE user_limits
    ADD CONSTRAINT user_limits_chk_2 CHECK (limit_type BETWEEN 0 AND 8);
