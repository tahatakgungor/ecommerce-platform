package com.ecommerce.product.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSchemaPatchRunnerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void run_shouldSkipWhenUsersTableMissing() {
        when(jdbcTemplate.queryForObject(contains("table_name = 'users'"), eq(Boolean.class)))
                .thenReturn(false);

        UserSchemaPatchRunner runner = new UserSchemaPatchRunner(jdbcTemplate);
        runner.run();

        verify(jdbcTemplate, never()).execute(contains("ALTER TABLE users"));
    }

    @Test
    void run_shouldApplyPatchWhenUsersTableExists() {
        when(jdbcTemplate.queryForObject(contains("table_name = 'users'"), eq(Boolean.class)))
                .thenReturn(true);

        UserSchemaPatchRunner runner = new UserSchemaPatchRunner(jdbcTemplate);
        runner.run();

        verify(jdbcTemplate).execute(contains("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token_expires_at"));
    }
}
