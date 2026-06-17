package tech.grastone.fz.matching.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class UserLimitsSchemaRepair implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE user_limits DROP CHECK user_limits_chk_2");
            log.info("Dropped stale user_limits_chk_2 check constraint");
        } catch (Exception error) {
            log.debug("user_limits_chk_2 drop skipped: {}", error.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE user_limits ADD CONSTRAINT user_limits_chk_2 CHECK (limit_type BETWEEN 0 AND 8)"
            );
            log.info("Updated user_limits_chk_2 check constraint for extended limit types");
        } catch (Exception error) {
            log.debug("user_limits_chk_2 add skipped: {}", error.getMessage());
        }
    }
}
