package com.bofa.cbp.auth.service;

import com.bofa.cbp.auth.JwtValidator;
import com.bofa.cbp.auth.RoleChecker;
import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import com.bofa.cbp.auth.domain.UserAccount;
import com.bofa.cbp.auth.domain.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates password-based login, admin-only registration, and
 * token introspection.
 */
@Service
public class AuthService {

    private final UserAccountRepository users;
    private final BCryptPasswordEncoder encoder;
    private final TokenService tokenService;
    private final JwtValidator validator;

    public AuthService(UserAccountRepository users,
                       BCryptPasswordEncoder encoder,
                       TokenService tokenService,
                       JwtValidator validator) {
        this.users = users;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.validator = validator;
    }

    @ComplianceCritical(
        category = ComplianceCategory.AUTHENTICATION,
        note = "Primary username+password login. Produces signed access + refresh tokens."
    )
    public LoginResult login(String username, String password) {
        UserAccount user = users.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("unknown user"));
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("bad password");
        }
        String access = tokenService.issueAccessToken(user.getUsername(), user.getRoles());
        String refresh = tokenService.issueRefreshToken(user.getUsername());
        return new LoginResult(access, refresh, user.getRoles());
    }

    @ComplianceCritical(
        category = ComplianceCategory.AUTHENTICATION,
        note = "Admin-only. Enforced via RoleChecker on the caller's token."
    )
    public UserAccount register(String callerToken, String newUsername, String newPassword, Set<String> roles) {
        JwtValidator.ValidationResult callerResult = validator.validate(callerToken);
        if (!callerResult.isValid()) {
            throw new ForbiddenException("caller token invalid: " + callerResult.reason());
        }
        if (!RoleChecker.hasAnyRole(callerResult.claims(), List.of("ADMIN"))) {
            throw new ForbiddenException("ADMIN role required");
        }
        if (users.findByUsername(newUsername).isPresent()) {
            throw new IllegalArgumentException("username already exists: " + newUsername);
        }
        UserAccount created = new UserAccount(
                newUsername,
                encoder.encode(newPassword),
                roles == null ? new HashSet<>() : new HashSet<>(roles)
        );
        return users.save(created);
    }

    @ComplianceCritical(
        category = ComplianceCategory.AUTHENTICATION,
        note = "Token introspection — hot path for every downstream Java service."
    )
    public IntrospectionResult introspect(String token) {
        JwtValidator.ValidationResult result = validator.validate(token);
        if (!result.isValid()) {
            return new IntrospectionResult(false, null, List.of(), result.reason());
        }
        List<String> roles = RoleChecker.rolesOf(result.claims());
        return new IntrospectionResult(true, result.subject(), roles, null);
    }

    public LoginResult refresh(String refreshToken) {
        JwtValidator.ValidationResult result = validator.validate(refreshToken);
        if (!result.isValid()) {
            throw new InvalidCredentialsException("refresh token invalid: " + result.reason());
        }
        Object typ = result.claims().get("typ");
        if (typ == null || !"refresh".equals(typ.toString())) {
            throw new InvalidCredentialsException("not a refresh token");
        }
        UserAccount user = users.findByUsername(result.subject())
                .orElseThrow(() -> new InvalidCredentialsException("user disappeared"));
        String access = tokenService.issueAccessToken(user.getUsername(), user.getRoles());
        String newRefresh = tokenService.issueRefreshToken(user.getUsername());
        return new LoginResult(access, newRefresh, user.getRoles());
    }

    public static final class LoginResult {
        public final String accessToken;
        public final String refreshToken;
        public final Set<String> roles;

        public LoginResult(String a, String r, Set<String> roles) {
            this.accessToken = a;
            this.refreshToken = r;
            this.roles = roles;
        }
    }

    public static final class IntrospectionResult {
        public final boolean active;
        public final String subject;
        public final List<String> roles;
        public final String reason;

        public IntrospectionResult(boolean active, String subject, List<String> roles, String reason) {
            this.active = active;
            this.subject = subject;
            this.roles = roles;
            this.reason = reason;
        }
    }
}
