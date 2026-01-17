package tech.grastone.fz.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FzMatchingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FzMatchingServiceApplication.class, args);
	}

}
