package tech.grastone.fz.matching.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.handler.SuccessResponseHandler;
import tech.grastone.fz.matching.service.PreferencesService;

@RestController
@RequestMapping("/pref")
@Slf4j
@AllArgsConstructor
public class PreferencesController {

	private final PreferencesService preferencesSevice;

	@PostMapping(path = "save", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SuccessResponseHandler<PreferencesDto>> savePref(Authentication authentication,
			@RequestBody PreferencesDto preferencesDto) {
		log.info("Saving user preferences");
		preferencesDto.setUserId(Long.parseLong(authentication.getPrincipal().toString()));

		return ResponseEntity.ok(new SuccessResponseHandler<PreferencesDto>(200, "Preferences has been updated !",
				preferencesSevice.save(preferencesDto)));
	}

	@GetMapping(path = "{userId}",produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SuccessResponseHandler<PreferencesDto>> fetchByIdPref(@PathVariable("userId") int userId){
		log.info("fetching user preferences");
		return ResponseEntity.ok(new SuccessResponseHandler<PreferencesDto>(200, "Preferences fetched successfully !", preferencesSevice.get(userId))) ;
	}
	
	@GetMapping(path = "me",produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SuccessResponseHandler<PreferencesDto>> fetchMePref(Authentication authentication){
		return fetchByIdPref(Integer.parseInt(authentication.getPrincipal().toString()));
	}
}
