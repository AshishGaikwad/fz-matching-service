package tech.grastone.fz.matching.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Frenzo Matching Service")
                        .version("1.0")
                        .description("To match users")
                        .contact(new Contact()
                                .name("Ashish Gaikwad")
                                .email("ashishgaikwad1997@gmail.com")));
    }
}