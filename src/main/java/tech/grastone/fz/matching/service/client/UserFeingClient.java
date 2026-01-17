package tech.grastone.fz.matching.service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import tech.grastone.fz.matching.config.FeignSecurityConfig;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.UserImageEntity;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;

@FeignClient(name= "FZ-USER-SERVICE",configuration = FeignSecurityConfig.class)
public interface UserFeingClient {

	@GetMapping(value = "/user/{id}",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuccessResponseHandler<UserDto>> getUser(@PathVariable("id") long userId);
	
	@GetMapping(value = "/image/fetch/profile/images/{userId}",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuccessResponseHandler<List<UserImageEntity>>> getUserImages(@PathVariable("userId") long userId);
	
}
