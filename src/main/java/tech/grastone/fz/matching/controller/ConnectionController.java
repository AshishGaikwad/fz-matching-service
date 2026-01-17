package tech.grastone.fz.matching.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.grastone.fz.matching.dto.ShowProfileDto;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.ConnectionService;

import java.util.List;

@RestController
@RequestMapping("/connection")
@AllArgsConstructor
@Slf4j
public class ConnectionController {

    private final ConnectionService connectionService;

    /**
     * GET /connection/{userId}
     * Admin or service call to fetch connections of a specific user.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<SuccessResponseHandler<List<ShowProfileDto>>> getConnectionsByUserId(@PathVariable long userId) {
        log.debug("API call: Fetching connections for userId={}", userId);
        List<ShowProfileDto> connections = connectionService.getConnections(userId);
        log.info("Fetched {} connections for userId={}", connections.size(), userId);
        return ResponseEntity.ok(
                new SuccessResponseHandler<>(200, "Connections fetched successfully", connections)
        );
    }

    /**
     * GET /connection/me
     * Authenticated user fetches their own connections.
     */
    @GetMapping("/me")
    public ResponseEntity<SuccessResponseHandler<List<ShowProfileDto>>> getMyConnections(Authentication authentication) {
        long userId = Long.parseLong(authentication.getPrincipal().toString());
        log.debug("Authenticated user requesting their connections. userId={}", userId);
        return getConnectionsByUserId(userId);
    }
}
