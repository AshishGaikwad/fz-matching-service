package tech.grastone.fz.matching.service.impl;

import java.util.Optional;

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
import tech.grastone.fz.matching.enums.Gender;
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

		Object result = null;
		switch (userDto.getSexualOrientation()) {
			case STRAIGHT: {
				if (userDto.getGender() == Gender.MALE) {
					log.debug("User is STRAIGHT MALE, using PreferencesMenDao");
					result = preferenceMenDao.save(modelMapper.map(preferencesDto, PreferencesMenEntity.class));
				} else if (userDto.getGender() == Gender.FEMALE) {
					log.debug("User is STRAIGHT FEMALE, using PreferencesWomenDao");
					result = preferenceWomenDao.save(modelMapper.map(preferencesDto, PreferencesWomenEntity.class));
				}
				break;
			}
			case GAY: {
				log.debug("User is GAY, using PreferencesGayDao");
				result = preferenceGayDao.save(modelMapper.map(preferencesDto, PreferencesGayEntity.class));
				break;
			}
			case LESBIAN: {
				log.debug("User is LESBIAN, using PreferencesLesbianDao");
				result = preferenceLesbianDao.save(modelMapper.map(preferencesDto, PreferencesLesbianEntity.class));
				break;
			}
			default:
				log.warn("Unknown sexual orientation for userId: {}", preferencesDto.getUserId());
				throw new ValidationException("Unsupported orientation");
		}

		log.info("Preferences saved successfully for userId: {}", preferencesDto.getUserId());
		return modelMapper.map(result, PreferencesDto.class);
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
		return modelMapper.map(result.get(), PreferencesDto.class);
	}
}
