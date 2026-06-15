package tech.grastone.fz.matching.service.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tech.grastone.fz.matching.config.FeignSecurityConfig;
import tech.grastone.fz.matching.dto.NotificationDto;
import tech.grastone.fz.matching.dto.VibeSocketEventDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.UserImageEntity;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;

@FeignClient(name= "FZ-MESSAGING-SERVICE")
public interface MessagingFeingClient {

	@PostMapping(value = "send-notification",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,Object>> sendNotification(@RequestBody NotificationDto notificationDto);

	@PostMapping(value = "broadcast-vibe-event", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String,Object>> broadcastVibeEvent(@RequestBody VibeSocketEventDto eventDto);

}
