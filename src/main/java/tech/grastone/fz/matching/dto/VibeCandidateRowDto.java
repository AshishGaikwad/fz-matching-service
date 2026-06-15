package tech.grastone.fz.matching.dto;

import lombok.Data;

@Data
public class VibeCandidateRowDto {
    private Long participationId;
    private Long sessionId;
    private Long vibeId;
    private double distanceKm;
    private int radiusKm;
    private UserDto user;
    private PreferencesDto preference;
}
