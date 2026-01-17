package tech.grastone.fz.matching.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class FeignSecurityConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                String token = extractTokenFromRequest();
                if (token != null) {
                    String bearerToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
                    template.header("Authorization", bearerToken);
                    log.debug("Forwarded Authorization header to Feign request for URL: {}", template.url());
                } else {
                    log.warn("No Authorization header found in the incoming request. Skipping token forwarding.");
                }
            }

            private String extractTokenFromRequest() {
                RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                    HttpServletRequest request = servletRequestAttributes.getRequest();
                    String token = request.getHeader("Authorization");
                    log.debug("Authorization header found: {}", token != null ? "[REDACTED]" : "null");
                    return token;
                } else {
                    log.warn("RequestAttributes are not of type ServletRequestAttributes — cannot extract token.");
                }
                return null;
            }
        };
    }
}
