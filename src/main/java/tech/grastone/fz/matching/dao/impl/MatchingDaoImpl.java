package tech.grastone.fz.matching.dao.impl;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import tech.grastone.fz.matching.dao.MatchingDao;
import tech.grastone.fz.matching.dto.MatchedByPreferencesDto;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.enums.Gender;
import tech.grastone.fz.matching.enums.Orientation;
import tech.grastone.fz.matching.util.QueryUtil;

@Repository
@AllArgsConstructor
public class MatchingDaoImpl implements MatchingDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final QueryUtil queryUtil;

    @Override
    public List<MatchedByPreferencesDto> getMatchedUserUsingPreferences(UserDto userDto, PreferencesDto preferencesDto, Pageable pageable) {
        String tableName = resolveTableName(userDto);

        String baseQuery = queryUtil.getQueryForMatchedUserUsingPreferences(tableName);

        // Add pagination
        String paginatedQuery = baseQuery + " LIMIT :limit OFFSET :offset";

        Map<String, Object> params = new HashMap<>();
        params.put("lattitude", userDto.getLattitude());
        params.put("longitude", userDto.getLongitude());
        params.put("drinking", preferencesDto.getDrinking().ordinal());
        params.put("lifestyle", preferencesDto.getLifestyle().ordinal());
        params.put("religion", preferencesDto.getReligion().ordinal());
        params.put("smoking", preferencesDto.getSmoking().ordinal());
        params.put("personality", preferencesDto.getPersonality().ordinal());
        params.put("minAge", preferencesDto.getMinAge());
        params.put("maxAge", preferencesDto.getMaxAge());
        params.put("maxDistance", preferencesDto.getDistance());
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());
        params.put("lat_rad", -65.963458);
        params.put("lon_rad", 80.289978);
        params.put("currentUserId", userDto.getId());
        return namedParameterJdbcTemplate.query(paginatedQuery, params, (ResultSet rs, int rowNum) -> {
            MatchedByPreferencesDto dto = new MatchedByPreferencesDto();
            dto.setUser_id(rs.getLong(1));
            dto.setDistance_km(rs.getDouble(2));
            dto.setMatching_per(rs.getDouble(3));
            return dto;
        });
    }

    private String resolveTableName(UserDto userDto) {
        Orientation orientation = userDto.getSexualOrientation();
        Gender gender = userDto.getGender();

        return switch (orientation) {
            case STRAIGHT -> (gender == Gender.MALE) ?  "preferences_women"  : "preferences_men" ;
            case GAY -> "preferences_gay";
            case LESBIAN -> "preferences_lesbian";
            default -> throw new IllegalArgumentException("Unsupported orientation: " + orientation);
        };
    }

}
