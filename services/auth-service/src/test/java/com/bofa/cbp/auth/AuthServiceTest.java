package com.bofa.cbp.auth;

import com.bofa.cbp.auth.service.AuthService;
import com.bofa.cbp.auth.service.AuthService.IntrospectionResult;
import com.bofa.cbp.auth.service.AuthService.LoginResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void adminSeedCanLoginAndIssuedTokenIntrospectsActive() {
        LoginResult login = authService.login("admin", "admin-seed-password");
        assertNotNull(login.accessToken);
        assertNotNull(login.refreshToken);
        assertTrue(login.roles.contains("ADMIN"));

        IntrospectionResult introspected = authService.introspect(login.accessToken);
        assertTrue(introspected.active, introspected.reason);
        assertEquals("admin", introspected.subject);
        assertTrue(introspected.roles.contains("ADMIN"));
    }
}
