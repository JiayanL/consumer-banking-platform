package com.bofa.cbp.account.config;

import com.bofa.cbp.auth.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtValidator jwtValidator(@Value("${cbp.auth.secret}") String secret) {
        return new JwtValidator(secret);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtValidator jwtValidator) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(new JwtAuthFilter(jwtValidator), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static class JwtAuthFilter extends OncePerRequestFilter {
        private final JwtValidator validator;

        JwtAuthFilter(JwtValidator validator) {
            this.validator = validator;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String header = request.getHeader("Authorization");
            if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = header.substring(7);
                JwtValidator.ValidationResult result = validator.validate(token);
                if (result.isValid()) {
                    SecurityContextHolder.getContext().setAuthentication(new JwtAuth(result.subject()));
                }
            }
            chain.doFilter(request, response);
        }
    }

    static class JwtAuth extends AbstractAuthenticationToken {
        private final String subject;

        JwtAuth(String subject) {
            super(List.of(new SimpleGrantedAuthority("ROLE_USER")));
            this.subject = subject;
            setAuthenticated(true);
        }

        @Override public Object getCredentials() { return ""; }
        @Override public Object getPrincipal() { return subject; }
    }
}
