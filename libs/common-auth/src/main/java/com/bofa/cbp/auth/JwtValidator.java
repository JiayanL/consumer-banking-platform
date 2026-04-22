package com.bofa.cbp.auth;

import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Validates HS256-signed JWTs issued by auth-service. Shared across
 * every Java service that needs to authenticate a request.
 */
public class JwtValidator {

    private final SecretKey key;
    private final Clock clock;

    public JwtValidator(String sharedSecret) {
        this(sharedSecret, Clock.systemUTC());
    }

    public JwtValidator(String sharedSecret, Clock clock) {
        Objects.requireNonNull(sharedSecret, "sharedSecret");
        if (sharedSecret.length() < 32) {
            throw new IllegalArgumentException("shared secret must be >= 32 chars");
        }
        this.key = Keys.hmacShaKeyFor(sharedSecret.getBytes());
        this.clock = clock;
    }

    @ComplianceCritical(
        category = ComplianceCategory.AUTHENTICATION,
        note = "Primary token validation entry point for all Java services."
    )
    public ValidationResult validate(String token) {
        if (token == null || token.isBlank()) {
            return ValidationResult.invalid("empty token");
        }
        try {
            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            Claims body = parsed.getBody();
            Date exp = body.getExpiration();
            if (exp == null) {
                return ValidationResult.invalid("missing exp claim");
            }
            Instant now = clock.instant();
            // Note: exactly-equal-to-now is treated as expired on the
            // Java side. The Node side treats it as still valid.
            if (!exp.toInstant().isAfter(now)) {
                return ValidationResult.invalid("token expired");
            }
            return ValidationResult.ok(body.getSubject(), body);
        } catch (Exception e) {
            return ValidationResult.invalid(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final String subject;
        private final Claims claims;
        private final String reason;

        private ValidationResult(boolean valid, String subject, Claims claims, String reason) {
            this.valid = valid;
            this.subject = subject;
            this.claims = claims;
            this.reason = reason;
        }

        public static ValidationResult ok(String subject, Claims claims) {
            return new ValidationResult(true, subject, claims, null);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, null, null, reason);
        }

        public boolean isValid() { return valid; }
        public String subject() { return subject; }
        public Claims claims() { return claims; }
        public String reason() { return reason; }
    }
}
