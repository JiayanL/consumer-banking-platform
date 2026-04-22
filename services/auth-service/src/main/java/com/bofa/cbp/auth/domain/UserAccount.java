package com.bofa.cbp.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles = new HashSet<>();

    public UserAccount() {}

    public UserAccount(String username, String passwordHash, Set<String> roles) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Set<String> getRoles() { return roles; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String v) { this.username = v; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public void setRoles(Set<String> v) { this.roles = v; }
}
