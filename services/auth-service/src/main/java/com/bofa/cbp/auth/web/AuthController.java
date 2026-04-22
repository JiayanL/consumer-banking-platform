package com.bofa.cbp.auth.web;

import com.bofa.cbp.auth.service.AuthService;
import com.bofa.cbp.auth.service.AuthService.IntrospectionResult;
import com.bofa.cbp.auth.service.AuthService.LoginResult;
import com.bofa.cbp.auth.service.ForbiddenException;
import com.bofa.cbp.auth.service.InvalidCredentialsException;
import com.bofa.cbp.auth.domain.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        LoginResult r = authService.login(req.username, req.password);
        Map<String, Object> out = new HashMap<>();
        out.put("accessToken", r.accessToken);
        out.put("refreshToken", r.refreshToken);
        out.put("roles", r.roles);
        return out;
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestBody RefreshRequest req) {
        LoginResult r = authService.refresh(req.refreshToken);
        Map<String, Object> out = new HashMap<>();
        out.put("accessToken", r.accessToken);
        out.put("refreshToken", r.refreshToken);
        return out;
    }

    @PostMapping("/introspect")
    public Map<String, Object> introspect(@RequestBody IntrospectRequest req) {
        IntrospectionResult result = authService.introspect(req.token);
        Map<String, Object> out = new HashMap<>();
        out.put("active", result.active);
        out.put("sub", result.subject);
        out.put("roles", result.roles);
        if (result.reason != null) {
            out.put("reason", result.reason);
        }
        return out;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestHeader("Authorization") String authorization,
            @RequestBody RegisterRequest req) {
        String token = authorization == null ? "" : authorization.replaceFirst("(?i)^Bearer\\s+", "");
        UserAccount created = authService.register(token, req.username, req.password, req.roles);
        Map<String, Object> out = new HashMap<>();
        out.put("id", created.getId());
        out.put("username", created.getUsername());
        out.put("roles", created.getRoles());
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class RefreshRequest {
        public String refreshToken;
    }

    public static class IntrospectRequest {
        public String token;
    }

    public static class RegisterRequest {
        public String username;
        public String password;
        public Set<String> roles;
    }
}
