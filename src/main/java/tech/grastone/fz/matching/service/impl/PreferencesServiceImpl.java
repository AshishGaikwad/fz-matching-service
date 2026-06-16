package tech.grastone.fz.matching.service.impl;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dao.PreferencesDao;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.PreferencesGayEntity;
import tech.grastone.fz.matching.entity.PreferencesLesbianEntity;
import tech.grastone.fz.matching.entity.PreferencesMenEntity;
import tech.grastone.fz.matching.entity.PreferencesWomenEntity;
import tech.grastone.fz.matching.entity.base.BasePreferenceEntity;
import tech.grastone.fz.matching.enums.Gender;
import tech.grastone.fz.matching.enums.LookingFor;
import tech.grastone.fz.matching.exception.DataNotFoundException;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.PreferencesService;
import tech.grastone.fz.matching.service.client.UserFeingClient;

@Service
@Slf4j
public class PreferencesServiceImpl implements PreferencesService {

	private final ModelMapper modelMapper;
	private final UserFeingClient userFeingClient;
	private final PreferencesDao preferenceMenDao;
	private final PreferencesDao preferenceWomenDao;
	private final PreferencesDao preferenceGayDao;
	private final PreferencesDao preferenceLesbianDao;

	public PreferencesServiceImpl(ModelMapper modelMapper, UserFeingClient userFeingClient,
			@Qualifier("preferencesMenDaoImpl") PreferencesDao preferenceMenDao,
			@Qualifier("preferencesWomenDaoImpl") PreferencesDao preferenceWomenDao,
			@Qualifier("preferencesGayDaoImpl") PreferencesDao preferenceGayDao,
			@Qualifier("preferencesLesbianDaoImpl") PreferencesDao preferenceLesbianDao) {
		this.modelMapper = modelMapper;
		this.userFeingClient = userFeingClient;
		this.preferenceMenDao = preferenceMenDao;
		this.preferenceWomenDao = preferenceWomenDao;
		this.preferenceGayDao = preferenceGayDao;
		this.preferenceLesbianDao = preferenceLesbianDao;
	}

	@Override
	public PreferencesDto save(PreferencesDto preferencesDto) {
		log.info("Saving preferences for userId: {}", preferencesDto.getUserId());
		UserDto userDto = getUserDetails(preferencesDto.getUserId());
		PreferencesDto normalized = normalizePreferences(preferencesDto, userDto);

		BasePreferenceEntity result = null;
		switch (userDto.getSexualOrientation()) {
			case STRAIGHT: {
				if (userDto.getGender() == Gender.MALE) {
					log.debug("User is STRAIGHT MALE, using PreferencesMenDao");
					result = savePreferences(normalized, PreferencesMenEntity.class, preferenceMenDao);
				} else if (userDto.getGender() == Gender.FEMALE) {
					log.debug("User is STRAIGHT FEMALE, using PreferencesWomenDao");
					result = savePreferences(normalized, PreferencesWomenEntity.class, preferenceWomenDao);
				}
				break;
			}
			case GAY: {
				log.debug("User is GAY, using PreferencesGayDao");
				result = savePreferences(normalized, PreferencesGayEntity.class, preferenceGayDao);
				break;
			}
			case LESBIAN: {
				log.debug("User is LESBIAN, using PreferencesLesbianDao");
				result = savePreferences(normalized, PreferencesLesbianEntity.class, preferenceLesbianDao);
				break;
			}
			default:
				log.warn("Unknown sexual orientation for userId: {}", preferencesDto.getUserId());
				throw new ValidationException("Unsupported orientation");
		}

		log.info("Preferences saved successfully for userId: {}", preferencesDto.getUserId());
		return toDto(result, userDto);
	}

	private UserDto getUserDetails(long userId) {
		log.debug("Fetching user details for userId: {}", userId);
		try {
			ResponseEntity<SuccessResponseHandler<UserDto>> response = userFeingClient.getUser(userId);
			UserDto userDto = response.getBody().getBody();
			log.debug("User details fetched successfully: {}", userDto);
			return userDto;
		} catch (Exception e) {
			log.error("Failed to fetch user details for userId: {}", userId, e);
			throw new DataNotFoundException("Invalid request! User not found");
		}
	}

	@Override
	public PreferencesDto get(int userId) {
		log.info("Fetching preferences for userId: {}", userId);
		UserDto userDto = getUserDetails(userId);
		Optional<?> result = Optional.empty();

		switch (userDto.getSexualOrientation()) {
			case STRAIGHT: {
				if (userDto.getGender() == Gender.MALE) {
					log.debug("Fetching STRAIGHT MALE preferences");
					result = preferenceMenDao.get(userId);
					
				} else if (userDto.getGender() == Gender.FEMALE) {
					
					
					log.debug("Fetching STRAIGHT FEMALE preferences");
					result = preferenceWomenDao.get(userId);
				}
				break;
			}
			case GAY:
				log.debug("Fetching GAY preferences");
				result = preferenceGayDao.get(userId);
				break;
			case LESBIAN:
				log.debug("Fetching LESBIAN preferences");
				result = preferenceLesbianDao.get(userId);
				break;
			default:
				log.warn("Unknown sexual orientation for userId: {}", userId);
		}

		if (result.isEmpty()) {
			log.warn("Preferences not found for userId: {}", userId);
			throw new ValidationException("Preferences not found. Please create preferences first");
		}

		log.info("Preferences fetched successfully for userId: {}", userId);
		return toDto((BasePreferenceEntity) result.get(), userDto);
	}

	private <T extends BasePreferenceEntity> T savePreferences(PreferencesDto preferencesDto, Class<T> entityClass,
			PreferencesDao dao) {
		T entity = modelMapper.map(preferencesDto, entityClass);
		entity.setLookingForValues(serializeLookingFor(preferencesDto.getLookingFor()));
		entity.setMaritalStatus(preferencesDto.getMaritalStatus());
		entity.setProfession(normalizeProfession(preferencesDto.getProfession()));
		return (T) dao.save(entity);
	}

	private PreferencesDto normalizePreferences(PreferencesDto preferencesDto, UserDto userDto) {
		PreferencesDto normalized = new PreferencesDto();
		normalized.setUserId(preferencesDto.getUserId());
		normalized.setMinAge(preferencesDto.getMinAge());
		normalized.setMaxAge(preferencesDto.getMaxAge());
		normalized.setDistance(preferencesDto.getDistance());
		normalized.setSmoking(preferencesDto.getSmoking());
		normalized.setDrinking(preferencesDto.getDrinking());
		normalized.setPersonality(preferencesDto.getPersonality());
		normalized.setReligion(preferencesDto.getReligion());
		normalized.setLifestyle(preferencesDto.getLifestyle());
		normalized.setMaritalStatus(firstNonNull(preferencesDto.getMaritalStatus(), userDto.getMaritalStatus()));
		normalized.setProfession(normalizeProfession(firstNonBlank(preferencesDto.getProfession(), userDto.getProfession())));
		normalized.setLookingFor(normalizeLookingFor(preferencesDto.getLookingFor(), userDto.getLookingFor()));
		return normalized;
	}

	private PreferencesDto toDto(BasePreferenceEntity entity, UserDto userDto) {
		PreferencesDto dto = modelMapper.map(entity, PreferencesDto.class);
		dto.setLookingFor(deserializeLookingFor(entity.getLookingForValues(), userDto.getLookingFor()));
		dto.setMaritalStatus(firstNonNull(entity.getMaritalStatus(), userDto.getMaritalStatus()));
		dto.setProfession(normalizeProfession(firstNonBlank(entity.getProfession(), userDto.getProfession())));
		return dto;
	}

	private String serializeLookingFor(Set<LookingFor> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.stream()
				.filter(value -> value != null)
				.map(Enum::name)
				.distinct()
				.limit(7)
				.sorted()
				.collect(Collectors.joining(","));
	}

	private Set<LookingFor> deserializeLookingFor(String values, Set<LookingFor> fallback) {
		if (values == null || values.isBlank()) {
			return fallback == null ? new LinkedHashSet<>() : new LinkedHashSet<>(fallback);
		}

		Set<LookingFor> parsed = new LinkedHashSet<>();
		for (String token : values.split(",")) {
			try {
				parsed.add(LookingFor.valueOf(token.trim()));
			} catch (IllegalArgumentException ignored) {
				// Skip unknown persisted values.
			}
		}
		return parsed.isEmpty() && fallback != null ? new LinkedHashSet<>(fallback) : parsed;
	}

	private Set<LookingFor> normalizeLookingFor(Set<LookingFor> requested, Set<LookingFor> fallback) {
		if (requested != null) {
			return requested.stream()
					.filter(value -> value != null)
					.limit(7)
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		return fallback == null ? new LinkedHashSet<>() : new LinkedHashSet<>(fallback);
	}

	private String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary.trim();
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback.trim();
		}
		return null;
	}

	private <T> T firstNonNull(T primary, T fallback) {
		return primary != null ? primary : fallback;
	}

	private String normalizeProfession(String profession) {
		if (profession == null) {
			return null;
		}

		String trimmed = profession.trim().replaceAll("\\s+", " ");
		if (trimmed.isEmpty()) {
			return null;
		}

		return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80);
	}
}
