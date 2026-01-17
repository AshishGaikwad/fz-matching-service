package tech.grastone.fz.matching.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalConfig {

	@Bean
	ModelMapper modelMapper() {
		return new ModelMapper();
	}
	
}
