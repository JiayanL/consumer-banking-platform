package com.bofa.cbp.auth.config;

import com.bofa.cbp.auth.domain.UserAccount;
import com.bofa.cbp.auth.domain.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdminSeedRunner implements CommandLineRunner {

    private final UserAccountRepository users;
    private final BCryptPasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminSeedRunner(UserAccountRepository users,
                           BCryptPasswordEncoder encoder,
                           @Value("${cbp.admin-seed.username:admin}") String username,
                           @Value("${cbp.admin-seed.password:admin-seed-password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        UserAccount admin = new UserAccount(username, encoder.encode(password), Set.of("ADMIN", "USER"));
        users.save(admin);
    }
}
