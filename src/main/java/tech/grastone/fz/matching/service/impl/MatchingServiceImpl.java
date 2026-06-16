package tech.grastone.fz.matching.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dao.*;
import tech.grastone.fz.matching.dto.*;
import tech.grastone.fz.matching.entity.*;
import tech.grastone.fz.matching.enums.*;
import tech.grastone.fz.matching.exception.*;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.MatchingService;
import tech.grastone.fz.matching.service.SafetyService;
import tech.grastone.fz.matching.service.PreferencesService;
import tech.grastone.fz.matching.service.client.MessagingFeingClient;
import tech.grastone.fz.matching.service.client.UserFeingClient;
import tech.grastone.fz.matching.util.CommonUtil;

@Service
@AllArgsConstructor
@Slf4j
public class MatchingServiceImpl implements MatchingService {

	private final UserFeingClient userFeingClient;
	private final MessagingFeingClient messagingFeingClient;
	private final PreferencesService preferencesService;
	private final MatchingDao matchingDao;
	private final UserLimitsDao limitsDao;
	private final CommonUtil commonUtil;
	private final UserMatchesDao userMatchesDao;
	private final MatchRequestDao matchRequestDao;
	private final ConnectionDao connectionDao;
	private final SafetyService safetyService;

	@Override
	public List<ShowProfileDto> getMatches(long userId, Pageable pageable) {
		log.info("Fetching matches for userId: {}, page: {}", userId, pageable.getPageNumber());

		UserDto userDto = getUserDetails(userId);
		log.debug("Fetched user details for userId {}: {}", userId, userDto);

		checkAndEnforceUserLimit(userId, userDto);
		log.debug("Checked and enforced user limits for userId {}", userId);

		Page<UserMatchesEntity> matchPage = userMatchesDao.findByUserIdAndStatusAndType(
				userId, MatchStatus.PENDING, MatchType.BASE, pageable);

		if (matchPage.isEmpty()) {
			log.info("No cached matches found. Generating new matches for userId: {}", userId);

			PreferencesDto preferencesDto = preferencesService.get((int) userId);
			log.debug("Fetched preferences for userId {}: {}", userId, preferencesDto);

			List<MatchedByPreferencesDto> matchedDtos = matchingDao.getMatchedUserUsingPreferences(
					userDto, preferencesDto, pageable);
			Set<Long> blockedDuringGeneration = safetyService.blockedUserIds(userId,
					matchedDtos == null ? List.of() : matchedDtos.stream().map(MatchedByPreferencesDto::getUser_id).toList());

			if (matchedDtos != null && !matchedDtos.isEmpty()) {
				log.info("Found {} new matches based on preferences for userId {}", matchedDtos.size(), userId);
				List<UserMatchesEntity> newMatches = new ArrayList<>();
				for (MatchedByPreferencesDto dto : matchedDtos) {
					if (blockedDuringGeneration.contains(dto.getUser_id())) {
						continue;
					}
					log.debug("Creating match entry for userId {} and matchedUserId {}", userId, dto.getUser_id());
					newMatches.add(createUserMatch(userId, dto.getUser_id()));
				}
				userMatchesDao.saveAll(newMatches);
				log.info("Saved {} new matches to DB for userId {}", newMatches.size(), userId);

				matchPage = userMatchesDao.findByUserIdAndStatusAndType(userId, MatchStatus.PENDING,
						MatchType.BASE, pageable);
			} else {
				log.info("No matches found using preferences for userId {}", userId);
			}
		}

		Set<Long> blockedIds = safetyService.blockedUserIds(userId,
				matchPage.getContent().stream().map(UserMatchesEntity::getUserId2).toList());
		List<ShowProfileDto> profiles = new ArrayList<>();

		if (matchPage.hasContent()) {
			log.info("Building ShowProfileDto for {} matches", matchPage.getNumberOfElements());

			matchPage.getContent().forEach(content -> {
				if (blockedIds.contains(content.getUserId2())) {
					return;
				}
				log.debug("Processing match: {}", content);

				UserDto matchedUser = getUserDetails(content.getUserId2());
				log.debug("Fetched matched user details: {}", matchedUser);

				PreferencesDto preferences = preferencesService.get((int) content.getUserId2());
				log.debug("Fetched preferences for matched userId {}: {}", content.getUserId2(), preferences);
				
				List<UserImageEntity> userImagesList = getUserImages(content.getUserId2());

				ShowProfileDto profile = new ShowProfileDto();
				profile.setUser(matchedUser);
				profile.setPreference(preferences);
				profile.setUserMatch(content);
				profile.setId(UUID.randomUUID().toString());
				profile.setUserImages(userImagesList);
				profiles.add(profile);
			});
		} else {
			log.info("No matches found for userId: {}", userId);
		}

		log.info("Returning {} profiles for userId {}", profiles.size(), userId);
		return profiles;
	}


	@Override
	public ShowProfileDto showProfile(long userId, long userMatchesId) {
		log.info("Loading profile for userMatchId: {} and userId: {}", userMatchesId, userId);

		UserMatchesEntity match = userMatchesDao.getById(userMatchesId);
		if (match == null || match.getUserId1() != userId) {
			throw new DataNotFoundException("Match not found or unauthorized access.");
		}
		safetyService.assertNotBlocked(userId, match.getUserId2());

		UserDto me = getUserDetails(userId);
		UserDto matchedUser = getUserDetails(match.getUserId2());

		if (isFreeUser(me) && match.getMatchStatus() == MatchStatus.VIEWED) {
			throw new DataLimitException("Free users can view a profile only once. Please upgrade your plan.");
		}

		PreferencesDto preferences = preferencesService.get((int) match.getUserId2());
		ShowProfileDto profile = new ShowProfileDto();
		profile.setUser(matchedUser);
		profile.setPreference(preferences);

		if (isFreeUser(me) && match.getMatchStatus() == MatchStatus.PENDING) {
			profile.setUserLimits(incrementUserLimit(userId));
		}

		match.setMatchStatus(MatchStatus.VIEWED);
		profile.setUserMatch(userMatchesDao.save(match));

		log.info("Profile view completed for userMatchId: {}", userMatchesId);
		return profile;
	}

	@Override
	public MatchRequestEntity sendRequest(SendMatchRequestDto dto) {
		validateIds(dto.getSenderId(), dto.getReceiverId());
		log.info("Sending match request: {} -> {}", dto.getSenderId(), dto.getReceiverId());

		safetyService.assertNotBlocked(dto.getSenderId(), dto.getReceiverId());
		assertNoExistingConnection(dto.getSenderId(), dto.getReceiverId());

		Long senderId = dto.getSenderId();
		Long receiverId = dto.getReceiverId();

		// Prevent resending within 7 days of rejection
		List<MatchRequestEntity> recentRejected = matchRequestDao.findBySenderIdAndReceiverId(senderId, receiverId)
				.stream().filter(req -> req.getRequestStatus() == RequestStatus.REJECT)
				.filter(req -> req.getUpdatedAt() != null
						&& req.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(7)))
				.toList();

		if (!recentRejected.isEmpty()) {
			throw new ValidationException("Request was rejected recently. Please wait 7 days before sending again.");
		}

		// Auto-accept if reverse pending request exists
		MatchRequestEntity reverse = findPendingRequest(receiverId, senderId);
		if (reverse != null) {
			log.info("Reverse request found. Auto-accepting request [ID: {}]", reverse.getMatchRequestId());
			ReplyMatchRequestDto replyDto = new ReplyMatchRequestDto();
			replyDto.setId(reverse.getMatchRequestId());
			replyDto.setRequestStatus(RequestStatus.ACCEPT);
			replyDto.setReplyMessage("Auto-accepted due to mutual interest.");
			return replyRequest(replyDto);
		}

		boolean alreadyPending = matchRequestDao.findBySenderIdAndReceiverId(senderId, receiverId).stream()
				.anyMatch(req -> req.getRequestStatus() == RequestStatus.PENDING);
		if (alreadyPending) {
			throw new ValidationException("Request is already pending.");
		}


        UserDto r = getUserDetails(receiverId);

        UserDto s = getUserDetails(senderId);
        messagingFeingClient.sendNotification(NotificationDto.builder()
                .toUserId(receiverId)
                .notificationTitle("You have a new connection request")
                .notificationMessage(s.getFullName() + " sent you a request.")
                .build());

		// Proceed with new request
		MatchRequestEntity req = new MatchRequestEntity();
		req.setSenderId(senderId);
		req.setReceiverId(receiverId);
		req.setRequestMessage(dto.getRequestMessage());
		prepareMatchRequestEntityForCreation(req);
		MatchRequestEntity saved = matchRequestDao.save(req);
		log.info("Created match_request row [id={}, senderId={}, receiverId={}, status={}]",
				saved.getMatchRequestId(), saved.getSenderId(), saved.getReceiverId(), saved.getRequestStatus());
		return saved;
	}

	@Override
	public MatchRequestEntity replyRequest(ReplyMatchRequestDto dto) {
		if (dto == null || dto.getId() == null) {
			throw new ValidationException("Match request ID must be provided");
		}

		MatchRequestEntity req = matchRequestDao.get(dto.getId())
				.orElseThrow(() -> new ValidationException("Match request not found"));

		if (req.getRequestStatus() != RequestStatus.PENDING) {
			throw new ValidationException("This request has already been responded to.");
		}

		validateIds(req.getSenderId(), req.getReceiverId());
		assertNoExistingConnection(req.getSenderId(), req.getReceiverId());

		req.setRequestStatus(dto.getRequestStatus());
		req.setReplyMessage(dto.getReplyMessage());
		req.setUpdatedAt(LocalDateTime.now());

		MatchRequestEntity updated = matchRequestDao.save(req);
		log.info("Match request updated successfully [ID: {}]", updated.getMatchRequestId());

		if (dto.getRequestStatus() == RequestStatus.ACCEPT) {
			createConnectionFromAcceptedRequest(req);
		}
		return updated;
	}

	@Override
	public List<ShowProfileDto> getSentRequest(long userId, Pageable pageable) {
		if (userId <= 0) {
			throw new IllegalArgumentException("Invalid userId: " + userId);
		}


		Page<MatchRequestEntity>  matchPage = 	matchRequestDao.findBySenderIdAndRequestStatus(
				userId,
				RequestStatus.PENDING,
				pageable
		);

		List<ShowProfileDto> profiles = new ArrayList<>();

		Set<Long> blockedIds = safetyService.blockedUserIds(userId,
				matchPage.getContent().stream().map(MatchRequestEntity::getReceiverId).toList());
		if(matchPage.hasContent() ){
			matchPage.getContent().stream().forEach((content)->{
				if (blockedIds.contains(content.getReceiverId())) {
					return;
				}
				List<UserImageEntity> userImagesList = getUserImages(content.getReceiverId());
				UserDto matchedUser = getUserDetails(content.getReceiverId());
				ShowProfileDto profile = new ShowProfileDto();
				profile.setUser(matchedUser);
				profile.setMatchRequests(content);
				profile.setId(UUID.randomUUID().toString());
				profile.setUserImages(userImagesList);
				profiles.add(profile);
			});
		}



		return profiles;

	}


	@Override
	public List<ShowProfileDto> getReceivedRequest(long userId, Pageable pageable) {
		if (userId <= 0) {
			throw new IllegalArgumentException("Invalid userId: " + userId);
		}

		Page<MatchRequestEntity>  matchPage =  matchRequestDao.findByReceiverIdAndRequestStatus(
				userId,
				RequestStatus.PENDING,
				pageable
		);

		List<ShowProfileDto> profiles = new ArrayList<>();

		Set<Long> blockedIds = safetyService.blockedUserIds(userId,
				matchPage.getContent().stream().map(MatchRequestEntity::getSenderId).toList());
		if(matchPage.hasContent() ){
			matchPage.getContent().stream().forEach((content)->{
				if (blockedIds.contains(content.getSenderId())) {
					return;
				}
				List<UserImageEntity> userImagesList = getUserImages(content.getSenderId());
				UserDto matchedUser = getUserDetails(content.getSenderId());
				ShowProfileDto profile = new ShowProfileDto();
				profile.setUser(matchedUser);
				profile.setMatchRequests(content);
				profile.setId(UUID.randomUUID().toString());
				profile.setUserImages(userImagesList);
				profiles.add(profile);
			});
		}

		return profiles;
	}

	// ----------- Utility methods -----------------

	private MatchRequestEntity findPendingRequest(Long senderId, Long receiverId) {
		return matchRequestDao.findBySenderIdAndReceiverId(senderId, receiverId).stream()
				.filter(req -> req.getRequestStatus() == RequestStatus.PENDING).findFirst().orElse(null);
	}

	private void validateIds(Long a, Long b) {
		if (a == null || b == null) {
			throw new ValidationException("Sender and receiver IDs are required");
		}
		if (a.equals(b)) {
			throw new ValidationException("You cannot perform this operation on yourself");
		}
	}

	private void assertNoExistingConnection(Long a, Long b) {
		Long u1 = Math.min(a, b);
		Long u2 = Math.max(a, b);
		if (!connectionDao.getConnectionByUserId1AndUserId2(u1, u2).isEmpty()) {
			throw new ValidationException("Connection already exists");
		}
	}

	private void prepareMatchRequestEntityForCreation(MatchRequestEntity entity) {
		entity.setRequestStatus(RequestStatus.PENDING);
		entity.setCreatedAt(LocalDateTime.now());
		if (entity.getCreatedBy() == null) {
			entity.setCreatedBy(String.valueOf(entity.getSenderId()));
		}
	}

	private void createConnectionFromAcceptedRequest(MatchRequestEntity request) {
		Long sender = request.getSenderId();
		Long receiver = request.getReceiverId();

		ConnectionsEntity connection = new ConnectionsEntity();
		connection.setUserId1(Math.min(sender, receiver));
		connection.setUserId2(Math.max(sender, receiver));
		connection.setActive(true);
		connection.setConnectedAt(LocalDateTime.now());
		connection.setSentBy(sender);
		connection.setAcceptedBy(receiver);
		connection.setStatus(ConnectionStatus.ACCEPTED);

		connectionDao.save(connection);

		try {

            UserDto r = getUserDetails(receiver);
			messagingFeingClient.sendNotification(NotificationDto.builder().toUserId(sender).notificationTitle("You have been matched").notificationMessage("Whooyaa! You have been matched with " + r.getFullName()).build());

			UserDto s = getUserDetails(sender);
			messagingFeingClient.sendNotification(NotificationDto.builder().toUserId(receiver).notificationTitle("You have been matched").notificationMessage("Whooyaa! You have been matched with "+s.getFullName()).build());

		} catch (Exception e) {
			log.error("failed to send notification");
		}
		log.info("Connection saved between {} and {}", sender, receiver);
	}

	private UserMatchesEntity createUserMatch(long userId, long matchedUserId) {
		UserMatchesEntity match = new UserMatchesEntity();
		match.setUserId1(userId);
		match.setUserId2(matchedUserId);
		match.setMatchType(MatchType.BASE);
		match.setMatchStatus(MatchStatus.PENDING);
		match.setCreatedAt(LocalDateTime.now());
		match.setCreatedBy(String.valueOf(userId));
		return match;
	}

	private void checkAndEnforceUserLimit(long userId, UserDto userDto) {
		if (!isFreeUser(userDto))
			return;

		String periodKey = commonUtil.getPeriod(Frequency.DAILY);
		UserLimitsEntity limits = limitsDao.getUserLimits(userId, Frequency.DAILY, LimitType.PROFILE_VIEW, periodKey);

		if (limits == null) {
			limits = new UserLimitsEntity();
			limits.setUserId(userId);
			limits.setFrequency(Frequency.DAILY);
			limits.setLimitType(LimitType.PROFILE_VIEW);
			limits.setPeriodKey(periodKey);
			limits.setUsageCount(0);
			limits.setLimitValue(30); // Default daily view limit
			limitsDao.save(limits);
		}

		if (limits.getUsageCount() >= limits.getLimitValue()) {
			throw new DataLimitException("Daily profile view limit exceeded");
		}
	}

	private UserLimitsEntity incrementUserLimit(long userId) {
		String periodKey = commonUtil.getPeriod(Frequency.DAILY);
		UserLimitsEntity limits = limitsDao.getUserLimits(userId, Frequency.DAILY, LimitType.PROFILE_VIEW, periodKey);
		if (limits != null) {
			limits.setUsageCount(limits.getUsageCount() + 1);
			return limitsDao.save(limits);
		}
		return null;
	}

	private boolean isFreeUser(UserDto user) {
		return user.getSubscriptionPlan() == SubscriptionPlan.FREE || user.getPlanExpiryDate() == null
				|| user.getPlanExpiryDate().isBefore(LocalDate.now());
	}

	private UserDto getUserDetails(long userId) {
		try {
			ResponseEntity<SuccessResponseHandler<UserDto>> response = userFeingClient.getUser(userId);
			UserDto user = Optional.ofNullable(response.getBody()).map(SuccessResponseHandler::getBody).orElse(null);
			if (user == null) {
				throw new DataNotFoundException("User not found for ID: " + userId);
			}
			return user;
		} catch (Exception e) {
			log.error("Failed to fetch user details for userId: {}", userId, e);
			throw new DataNotFoundException("User not found or external service failed");
		}
	}
	
	private List<UserImageEntity> getUserImages(long userId) {
		try {
			ResponseEntity<SuccessResponseHandler<List<UserImageEntity>>> response = userFeingClient.getUserImages(userId);
			List<UserImageEntity> userImagesList = Optional.ofNullable(response.getBody()).map(SuccessResponseHandler::getBody).orElse(null);
			if (userImagesList == null) {
				throw new DataNotFoundException("User Images not found for ID: " + userId);
			}
			return userImagesList;
		} catch (Exception e) {
			log.error("Failed to fetch user details for userId: {}", userId, e);
			throw new DataNotFoundException("User not found or external service failed");
		}
	}
}
