package com.paper.mes.auth.config;

import com.paper.mes.auth.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthBootstrapTest {

    @Test
    void run_whenProdMissingInitialPassword_throwsBeforeUserInsert() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordService passwordService = mock(PasswordService.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).thenReturn(0);

        AuthBootstrap bootstrap = new AuthBootstrap(jdbcTemplate, passwordService, environment);

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        verify(jdbcTemplate, never()).update(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
