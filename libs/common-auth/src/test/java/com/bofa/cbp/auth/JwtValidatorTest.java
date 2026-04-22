package com.bofa.cbp.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorTest {

    private static final String SECRET = "test-secret-value-must-be-at-least-32-chars-long!";

    private String tokenExpiring(Instant exp) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder()
                .setSubject("user-1")
                .claim("roles", List.of("customer", "kyc:read"))
                .setExpiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    @Test
    void rejectsEmptyToken() {
        JwtValidator v = new JwtValidator(SECRET);
        assertFalse(v.validate("").isValid());
        assertFalse(v.validate(null).isValid());
    }

    @Test
    void acceptsFutureDatedToken() {
        JwtValidator v = new JwtValidator(SECRET);
        String t = tokenExpiring(Instant.now().plusSeconds(60));
        var r = v.validate(t);
        assertTrue(r.isValid());
        assertEquals("user-1", r.subject());
    }

    @Test
    void rejectsPastDatedToken() {
        JwtValidator v = new JwtValidator(SECRET);
        String t = tokenExpiring(Instant.now().minusSeconds(5));
        assertFalse(v.validate(t).isValid());
    }

    @Test
    void treatsExactNowAsExpired() {
        Instant fixed = Instant.parse("2024-01-01T00:00:00Z");
        Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);
        JwtValidator v = new JwtValidator(SECRET, clock);
        String t = tokenExpiring(fixed);
        var r = v.validate(t);
        assertFalse(r.isValid(), "exp == now is expired on Java side");
    }

    @Test
    void rolesAreParsed() {
        JwtValidator v = new JwtValidator(SECRET);
        String t = tokenExpiring(Instant.now().plusSeconds(60));
        var r = v.validate(t);
        assertTrue(RoleChecker.hasAnyRole(r.claims(), List.of("customer")));
        assertFalse(RoleChecker.hasAllRoles(r.claims(), List.of("customer", "admin")));
    }

    @Test
    void shortSecretRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtValidator("short"));
    }

    @Test
    void garbageTokenInvalid() {
        JwtValidator v = new JwtValidator(SECRET);
        assertFalse(v.validate("not.a.jwt").isValid());
    }
}
