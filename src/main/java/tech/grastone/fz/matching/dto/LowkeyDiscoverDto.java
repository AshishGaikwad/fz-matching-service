package tech.grastone.fz.matching.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.entity.UserImageEntity;

@Data
public class LowkeyDiscoverDto {
    private String id;
    private Long sessionId;
    private UserDto user;
    private PreferencesDto preference;
    private List<UserImageEntity> userImages;
    private MatchRequestEntity matchRequests;
    private double distanceKm;
    private int compatibilityScore;
    private int compatibilityPercentage;
    private String matchGrade;
    private String matchExplanation;
    private Map<String, Integer> scoreBreakdown;
    private int freshnessScore;
    private List<String> badges;
    private String onlineStatus;
    private String requestStatus;
}
