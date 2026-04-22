package com.bofa.cbp.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issues HS256-signed tokens using the same secret format expected by
 * common-auth's JwtValidator.
 */
@Service
public class TokenService {

    private final SecretKey signingKey;
    private final long accessTtlMinutes;
    private final long refreshTtlMinutes;

    public TokenService(
            @Value("${cbp.auth.secret}") String secret,
            @Value("${cbp.auth.access-token-ttl-minutes:15}") long accessTtlMinutes,
            @Value("${cbp.auth.refresh-token-ttl-minutes:1440}") long refreshTtlMinutes) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("cbp.auth.secret must be at least 32 chars");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTtlMinutes = accessTtlMinutes;
        this.refreshTtlMinutes = refreshTtlMinutes;
    }

    public String issueAccessToken(String subject, Collection<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlMinutes * 60);
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(Map.of("roles", List.copyOf(roles), "typ", "access"))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setId(UUID.randomUUID().toString())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String issueRefreshToken(String subject) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTtlMinutes * 60);
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(Map.of("typ", "refresh"))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setId(UUID.randomUUID().toString())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
