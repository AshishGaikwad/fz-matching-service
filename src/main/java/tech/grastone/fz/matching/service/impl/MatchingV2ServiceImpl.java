package tech.grastone.fz.matching.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dao.ConnectionDao;
import tech.grastone.fz.matching.dao.MatchRequestDao;
import tech.grastone.fz.matching.dao.MatchingDao;
import tech.grastone.fz.matching.dao.UserLimitsDao;
import tech.grastone.fz.matching.dao.UserMatchesDao;
import tech.grastone.fz.matching.dto.MatchedByPreferencesDto;
import tech.grastone.fz.matching.dto.NotificationDto;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.ReplyMatchRequestDto;
import tech.grastone.fz.matching.dto.SendMatchRequestDto;
import tech.grastone.fz.matching.dto.ShowProfileDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.ConnectionsEntity;
import tech.grastone.fz.matching.entity.MatchRequestEntity;
import tech.grastone.fz.matching.entity.UserImageEntity;
import tech.grastone.fz.matching.entity.UserLimitsEntity;
import tech.grastone.fz.matching.entity.UserMatchesEntity;
import tech.grastone.fz.matching.enums.ConnectionStatus;
import tech.grastone.fz.matching.enums.Frequency;
import tech.grastone.fz.matching.enums.LimitType;
import tech.grastone.fz.matching.enums.MatchStatus;
import tech.grastone.fz.matching.enums.MatchType;
import tech.grastone.fz.matching.enums.RequestStatus;
import tech.grastone.fz.matching.enums.SubscriptionPlan;
import tech.grastone.fz.matching.exception.DataLimitException;
import tech.grastone.fz.matching.exception.DataNotFoundException;
import tech.grastone.fz.matching.exception.ValidationException;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.MatchingV2Service;
import tech.grastone.fz.matching.service.PreferencesService;
import tech.grastone.fz.matching.service.SafetyService;
import tech.grastone.fz.matching.service.client.MessagingFeingClient;
import tech.grastone.fz.matching.service.client.UserFeingClient;
import tech.grastone.fz.matching.util.CommonUtil;

@Service
@AllArgsConstructor
@Slf4j
public class MatchingV2ServiceImpl implements MatchingV2Service {

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
		UserDto userDto = getUserDetails(userId);
		checkAndEnforceUserLimit(userId, userDto);

		Page<UserMatchesEntity> matchPage = userMatchesDao.findByUserIdAndStatusAndType(
				userId, MatchStatus.PENDING, MatchType.BASE, pageable);

		List<UserMatchesEntity> matches = new ArrayList<>(matchPage.getContent());
		Set<Long> blockedIds = safetyService.blockedUserIds(userId,
				matches.stream().map(UserMatchesEntity::getUserId2).toList());
		matches.removeIf(match -> blockedIds.contains(match.getUserId2()));
		if (matches.isEmpty()) {
			PreferencesDto preferencesDto = preferencesService.get((int) userId);
			List<MatchedByPreferencesDto> matchedDtos = matchingDao.getMatchedUserUsingPreferences(
					userDto, preferencesDto, pageable);

			if (matchedDtos != null && !matchedDtos.isEmpty()) {
				List<UserMatchesEntity> newMatches = new ArrayList<>();
				for (MatchedByPreferencesDto dto : matchedDtos) {
					if (safetyService.isBlocked(userId, dto.getUser_id())) {
						continue;
					}
					UserMatchesEntity match = createUserMatch(userId, dto.getUser_id());
					newMatches.add(match);
					matches.add(match);
				}
				userMatchesDao.saveAll(newMatches);
			}
		}

		return hydrateMatchProfiles(matches);
	}

	@Override
	public ShowProfileDto showProfile(long userId, long userMatchesId) {
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

		PreferencesDto preferences = preferencesService.get(Math.toIntExact(match.getUserId2()));
		ShowProfileDto profile = new ShowProfileDto();
		profile.setUser(matchedUser);
		profile.setPreference(preferences);
		profile.setUserImages(getUserImages(match.getUserId2()));

		if (isFreeUser(me) && match.getMatchStatus() == MatchStatus.PENDING) {
			profile.setUserLimits(incrementUserLimit(userId));
		}

		match.setMatchStatus(MatchStatus.VIEWED);
		profile.setUserMatch(userMatchesDao.save(match));
		return profile;
	}

	@Override
	public MatchRequestEntity sendRequest(SendMatchRequestDto dto) {
		validateIds(dto.getSenderId(), dto.getReceiverId());
		safetyService.assertNotBlocked(dto.getSenderId(), dto.getReceiverId());
		assertNoExistingConnection(dto.getSenderId(), dto.getReceiverId());

		Long senderId = dto.getSenderId();
		Long receiverId = dto.getReceiverId();

		List<MatchRequestEntity> recentRejected = matchRequestDao.findBySenderIdAndReceiverId(senderId, receiverId)
				.stream().filter(req -> req.getRequestStatus() == RequestStatus.REJECT)
				.filter(req -> req.getUpdatedAt() != null
						&& req.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(7)))
				.toList();

		if (!recentRejected.isEmpty()) {
			throw new ValidationException("Request was rejected recently. Please wait 7 days before sending again.");
		}

		MatchRequestEntity reverse = findPendingRequest(receiverId, senderId);
		if (reverse != null) {
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

		UserDto receiver = getUserDetails(receiverId);
		UserDto sender = getUserDetails(senderId);
		if (receiver.getSubscriptionPlan() == SubscriptionPlan.PREMIUM) {
			messagingFeingClient.sendNotification(NotificationDto.builder().toUserId(receiverId)
					.notificationTitle("You have new crush request!")
					.notificationMessage("Hey ! " + sender.getFullName() + " has seen you and sent you a request.")
					.build());
		}

		MatchRequestEntity req = new MatchRequestEntity();
		req.setSenderId(senderId);
		req.setReceiverId(receiverId);
		req.setRequestMessage(dto.getRequestMessage());
		prepareMatchRequestEntityForCreation(req);
		return matchRequestDao.save(req);
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
		if (dto.getRequestStatus() == RequestStatus.ACCEPT) {
			createConnectionFromAcceptedRequest(req);
		}
		return updated;
	}

	@Override
	public List<ShowProfileDto> getSentRequest(long userId, Pageable pageable) {
		Page<MatchRequestEntity> matchPage = matchRequestDao.findBySenderIdAndRequestStatus(userId,
				RequestStatus.PENDING, pageable);
		Set<Long> blockedIds = safetyService.blockedUserIds(userId,
				matchPage.getContent().stream().map(MatchRequestEntity::getReceiverId).toList());
		List<ShowProfileDto> profiles = new ArrayList<>();
		Map<Long, UserDto> userCache = new HashMap<>();
		Map<Long, List<UserImageEntity>> imageCache = new HashMap<>();
		for (MatchRequestEntity content : matchPage.getContent()) {
			if (blockedIds.contains(content.getReceiverId())) {
				continue;
			}
			Long receiverId = content.getReceiverId();
			ShowProfileDto profile = new ShowProfileDto();
			profile.setUser(userCache.computeIfAbsent(receiverId, this::getUserDetails));
			profile.setMatchRequests(content);
			profile.setId(UUID.randomUUID().toString());
			profile.setUserImages(imageCache.computeIfAbsent(receiverId, this::getUserImages));
			profiles.add(profile);
		}
		return profiles;
	}

	@Override
	public List<ShowProfileDto> getReceivedRequest(long userId, Pageable pageable) {
		Page<MatchRequestEntity> matchPage = matchRequestDao.findByReceiverIdAndRequestStatus(userId,
				RequestStatus.PENDING, pageable);
		Set<Long> blockedIds = safetyService.blockedUserIds(userId,
				matchPage.getContent().stream().map(MatchRequestEntity::getSenderId).toList());
		List<ShowProfileDto> profiles = new ArrayList<>();
		Map<Long, UserDto> userCache = new HashMap<>();
		Map<Long, List<UserImageEntity>> imageCache = new HashMap<>();
		for (MatchRequestEntity content : matchPage.getContent()) {
			if (blockedIds.contains(content.getSenderId())) {
				continue;
			}
			Long senderId = content.getSenderId();
			ShowProfileDto profile = new ShowProfileDto();
			profile.setUser(userCache.computeIfAbsent(senderId, this::getUserDetails));
			profile.setMatchRequests(content);
			profile.setId(UUID.randomUUID().toString());
			profile.setUserImages(imageCache.computeIfAbsent(senderId, this::getUserImages));
			profiles.add(profile);
		}
		return profiles;
	}

	private List<ShowProfileDto> hydrateMatchProfiles(List<UserMatchesEntity> matches) {
		List<ShowProfileDto> profiles = new ArrayList<>();
		Map<Long, UserDto> userCache = new HashMap<>();
		Map<Long, PreferencesDto> preferencesCache = new HashMap<>();
		Map<Long, List<UserImageEntity>> imageCache = new HashMap<>();

		for (UserMatchesEntity match : matches) {
			long matchedUserId = match.getUserId2();
			ShowProfileDto profile = new ShowProfileDto();
			profile.setUser(userCache.computeIfAbsent(matchedUserId, this::getUserDetails));
			profile.setPreference(preferencesCache.computeIfAbsent(matchedUserId,
					id -> preferencesService.get(Math.toIntExact(id))));
			profile.setUserMatch(match);
			profile.setId(UUID.randomUUID().toString());
			profile.setUserImages(imageCache.computeIfAbsent(matchedUserId, this::getUserImages));
			profiles.add(profile);
		}
		return profiles;
	}

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
			UserDto receiverUser = getUserDetails(receiver);
			messagingFeingClient.sendNotification(NotificationDto.builder().toUserId(sender)
					.notificationTitle("You have been matched")
					.notificationMessage("Whooyaa! You have been matched with " + receiverUser.getFullName())
					.build());

			UserDto senderUser = getUserDetails(sender);
			messagingFeingClient.sendNotification(NotificationDto.builder().toUserId(receiver)
					.notificationTitle("You have been matched")
					.notificationMessage("Whooyaa! You have been matched with " + senderUser.getFullName())
					.build());
		} catch (Exception e) {
			log.error("failed to send notification", e);
		}
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
			limits.setLimitValue(30);
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
			throw new DataNotFoundException("User not found or external service failed");
		}
	}
}
