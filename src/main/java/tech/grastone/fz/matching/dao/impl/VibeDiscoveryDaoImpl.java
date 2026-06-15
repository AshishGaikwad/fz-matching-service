package tech.grastone.fz.matching.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.VibeDiscoveryDao;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.dto.VibeCandidateRowDto;
import tech.grastone.fz.matching.enums.Drinking;
import tech.grastone.fz.matching.enums.Gender;
import tech.grastone.fz.matching.enums.Lifestyle;
import tech.grastone.fz.matching.enums.Orientation;
import tech.grastone.fz.matching.enums.Personality;
import tech.grastone.fz.matching.enums.Religion;
import tech.grastone.fz.matching.enums.Smoking;
import tech.grastone.fz.matching.enums.SubscriptionPlan;
import tech.grastone.fz.matching.enums.UserStatus;

@Repository
@AllArgsConstructor
public class VibeDiscoveryDaoImpl implements VibeDiscoveryDao {

    private static final int ZERO_KM_TEST_RADIUS_KM = 5;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<VibeCandidateRowDto> discoverCandidates(UserDto currentUser, Long sessionId, double latitude,
            double longitude, int radiusKm, Pageable pageable) {
        String preferenceTable = resolvePreferenceTable(currentUser);
        int effectiveRadiusKm = Math.max(ZERO_KM_TEST_RADIUS_KM, radiusKm);
        double latDelta = effectiveRadiusKm / 111.0;
        double lonBase = Math.max(0.1, Math.cos(Math.toRadians(latitude)));
        double lonDelta = effectiveRadiusKm / (111.0 * lonBase);

        String sql = """
                SELECT
                    p.participation_id,
                    p.session_id,
                    p.vibe_id,
                    p.user_id,
                    p.radius_km,
                    u.full_name,
                    u.email,
                    u.mobile,
                    u.dob,
                    u.gender,
                    u.sexual_orientation,
                    u.bio,
                    u.status AS user_status,
                    u.profile_pic_url,
                    u.lattitude,
                    u.longitude,
                    u.subscription_plan,
                    u.plan_expiry_date,
                    pref.min_age,
                    pref.max_age,
                    pref.distance,
                    pref.smoking,
                    pref.drinking,
                    pref.personality,
                    pref.religion,
                    pref.lifestyle,
                    (
                        6371 * ACOS(
                            LEAST(1, GREATEST(-1,
                                COS(RADIANS(:latitude)) * COS(RADIANS(p.latitude)) *
                                COS(RADIANS(p.longitude) - RADIANS(:longitude)) +
                                SIN(RADIANS(:latitude)) * SIN(RADIANS(p.latitude))
                            ))
                        )
                    ) AS distance_km
                FROM user_vibe_participation p
                JOIN users u ON u.id = p.user_id
                LEFT JOIN {{preferenceTable}} pref ON pref.user_id = p.user_id
                WHERE p.session_id = :sessionId
                  AND p.status = 'ACTIVE'
                  AND p.expires_at > NOW()
                  AND p.user_id <> :currentUserId
                  AND NOT EXISTS (
                      SELECT 1
                      FROM connections c
                      WHERE c.user_id1 = LEAST(:currentUserId, p.user_id)
                        AND c.user_id2 = GREATEST(:currentUserId, p.user_id)
                  )
                  AND p.latitude BETWEEN :minLat AND :maxLat
                  AND p.longitude BETWEEN :minLon AND :maxLon
                HAVING distance_km <= :radiusKm
                ORDER BY distance_km ASC, p.joined_at DESC
                LIMIT :limit OFFSET :offset
                """.replace("{{preferenceTable}}", preferenceTable);

        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", sessionId);
        params.put("currentUserId", currentUser.getId());
        params.put("latitude", latitude);
        params.put("longitude", longitude);
        params.put("radiusKm", effectiveRadiusKm);
        params.put("zeroKmTestRadiusKm", ZERO_KM_TEST_RADIUS_KM);
        params.put("minLat", latitude - latDelta);
        params.put("maxLat", latitude + latDelta);
        params.put("minLon", longitude - lonDelta);
        params.put("maxLon", longitude + lonDelta);
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        return jdbcTemplate.query(sql, params, this::mapCandidate);
    }

    private VibeCandidateRowDto mapCandidate(ResultSet rs, int rowNum) throws SQLException {
        UserDto user = new UserDto();
        user.setId(rs.getLong("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setMobile(rs.getString("mobile"));
        user.setDob(toLocalDate(rs, "dob"));
        user.setGender(enumByName(Gender.class, rs.getString("gender")));
        user.setSexualOrientation(enumByName(Orientation.class, rs.getString("sexual_orientation")));
        user.setBio(rs.getString("bio"));
        user.setStatus(enumByName(UserStatus.class, rs.getString("user_status")));
        user.setProfilePicUrl(rs.getString("profile_pic_url"));
        user.setLattitude(rs.getDouble("lattitude"));
        user.setLongitude(rs.getDouble("longitude"));
        user.setSubscriptionPlan(enumByName(SubscriptionPlan.class, rs.getString("subscription_plan")));
        user.setPlanExpiryDate(toLocalDate(rs, "plan_expiry_date"));

        PreferencesDto preference = new PreferencesDto();
        preference.setUserId(user.getId());
        preference.setMinAge(getInt(rs, "min_age", 18));
        preference.setMaxAge(getInt(rs, "max_age", 99));
        preference.setDistance(getInt(rs, "distance", 50));
        preference.setSmoking(enumByOrdinal(Smoking.class, getNullableInt(rs, "smoking")));
        preference.setDrinking(enumByOrdinal(Drinking.class, getNullableInt(rs, "drinking")));
        preference.setPersonality(enumByOrdinal(Personality.class, getNullableInt(rs, "personality")));
        preference.setReligion(enumByOrdinal(Religion.class, getNullableInt(rs, "religion")));
        preference.setLifestyle(enumByOrdinal(Lifestyle.class, getNullableInt(rs, "lifestyle")));

        VibeCandidateRowDto candidate = new VibeCandidateRowDto();
        candidate.setParticipationId(rs.getLong("participation_id"));
        candidate.setSessionId(rs.getLong("session_id"));
        candidate.setVibeId(rs.getLong("vibe_id"));
        candidate.setRadiusKm(rs.getInt("radius_km"));
        candidate.setDistanceKm(round(rs.getDouble("distance_km")));
        candidate.setUser(user);
        candidate.setPreference(preference);
        return candidate;
    }

    private String resolvePreferenceTable(UserDto userDto) {
        Orientation orientation = userDto.getSexualOrientation();
        Gender gender = userDto.getGender();

        if (orientation == Orientation.GAY) {
            return "preferences_gay";
        }
        if (orientation == Orientation.LESBIAN) {
            return "preferences_lesbian";
        }
        if (orientation == Orientation.STRAIGHT && gender == Gender.MALE) {
            return "preferences_women";
        }
        return "preferences_men";
    }

    private LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private int getInt(ResultSet rs, String column, int defaultValue) throws SQLException {
        Integer value = getNullableInt(rs, column);
        return value == null ? defaultValue : value;
    }

    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private <E extends Enum<E>> E enumByName(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private <E extends Enum<E>> E enumByOrdinal(Class<E> enumClass, Integer ordinal) {
        if (ordinal == null || ordinal < 0 || ordinal >= enumClass.getEnumConstants().length) {
            return null;
        }
        return enumClass.getEnumConstants()[ordinal];
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
