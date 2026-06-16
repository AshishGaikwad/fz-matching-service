package tech.grastone.fz.matching.service.client;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.cloud.openfeign.FeignClient;

import tech.grastone.fz.matching.config.FeignSecurityConfig;
import tech.grastone.fz.matching.dto.BlockCheckRequestDto;
import tech.grastone.fz.matching.dto.BlockCheckResponseDto;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;

@FeignClient(contextId = "userSafetyFeingClient", name = "FZ-USER-SERVICE", configuration = FeignSecurityConfig.class)
public interface UserSafetyFeingClient {

    @PostMapping(value = "/internal/safety/blocks/check-batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SuccessResponseHandler<BlockCheckResponseDto>> checkBlockedUsers(@RequestBody BlockCheckRequestDto request);
}
